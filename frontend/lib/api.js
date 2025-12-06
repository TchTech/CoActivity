// Simple API helper for the CoActivity backend
// Uses fetch and maps to the existing Spring Boot endpoints.

// Use current origin in browser (Next.js will proxy /api/* via rewrite)
// Use absolute URL in server-side rendering
const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ||
  (typeof window !== "undefined"
    ? window.location.origin // Use current origin - Next.js rewrite will handle /api/*
    : "http://localhost:8080") // Use absolute URL in SSR

async function request(path, { method = "GET", params, body, headers } = {}) {
  // Ensure path is a string, not an object
  const pathStr = typeof path === "string" ? path : String(path)
  if (pathStr.includes("[object")) {
    console.error(`[API] Invalid path detected: ${pathStr}. Original path:`, path)
    throw new Error(`Invalid API path: path must be a string, got ${typeof path}`)
  }
  
  const url = new URL(pathStr, API_BASE)
  console.log(`[API] ${method} ${url.toString()}`)

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        // Ensure value is converted to string safely
        let stringValue
        if (typeof value === "object") {
          // If it's an object, try to extract an ID or stringify
          stringValue = value.id || value.userId || value.postId || value.roomId || JSON.stringify(value)
        } else {
          stringValue = String(value)
        }
        
        if (!stringValue.includes("[object")) {
          url.searchParams.append(key, stringValue)
        } else {
          console.error(`[API] Invalid param value for ${key}:`, value)
        }
      }
    })
  }

  let bodyString = undefined
  if (body) {
    if (body instanceof FormData) {
      bodyString = body
    } else {
      // Убеждаемся, что body является объектом перед сериализацией
      if (typeof body === 'object' && body !== null) {
        bodyString = JSON.stringify(body)
        console.log(`[API] Request body (stringified):`, bodyString)
        console.log(`[API] Request body (object):`, body)
      } else {
        console.error(`[API] Invalid body type:`, typeof body, body)
        bodyString = String(body)
      }
    }
  }

  const options = {
    method,
    headers: {
      ...(body && !(body instanceof FormData)
        ? { "Content-Type": "application/json" }
        : {}),
      ...headers,
    },
    body: bodyString,
  }

  console.log(`[API] Fetch options:`, {
    method: options.method,
    url: url.toString(),
    hasBody: !!bodyString,
    bodyType: bodyString ? (bodyString instanceof FormData ? 'FormData' : typeof bodyString) : 'none',
    headers: options.headers
  })

  const res = await fetch(url.toString(), options)
  console.log(`[API] Response status: ${res.status} ${res.statusText}`)

  // Some endpoints legitimately return empty bodies
  const text = await res.text()
  console.log(`[API] Response text length: ${text.length}, first 500 chars:`, text.substring(0, 500))
  let data = null
  
  // Try to parse JSON only if there's content
  if (text && text.trim().length > 0) {
    try {
      const contentType = res.headers.get("content-type") || ""
      const trimmed = text.trim()
      
      // Check if response is JSON
      const isJsonContentType = contentType.includes("application/json")
      const looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[")
      
      if (isJsonContentType || looksLikeJson) {
        // Try to extract valid JSON from the response
        // Sometimes responses might have extra content before/after JSON
        let jsonText = trimmed
        
        // If response starts with JSON, try to find where it ends
        if (trimmed.startsWith("[")) {
          // For arrays, find the matching closing bracket
          let bracketCount = 0
          let inString = false
          let escapeNext = false
          let jsonEnd = trimmed.length
          
          for (let i = 0; i < trimmed.length; i++) {
            const char = trimmed[i]
            
            if (escapeNext) {
              escapeNext = false
              continue
            }
            
            if (char === "\\") {
              escapeNext = true
              continue
            }
            
            if (char === '"') {
              inString = !inString
              continue
            }
            
            if (!inString) {
              if (char === "[") bracketCount++
              if (char === "]") {
                bracketCount--
                if (bracketCount === 0) {
                  jsonEnd = i + 1
                  break
                }
              }
            }
          }
          
          jsonText = trimmed.substring(0, jsonEnd)
        } else if (trimmed.startsWith("{")) {
          // For objects, find the matching closing brace
          let braceCount = 0
          let inString = false
          let escapeNext = false
          let jsonEnd = trimmed.length
          
          for (let i = 0; i < trimmed.length; i++) {
            const char = trimmed[i]
            
            if (escapeNext) {
              escapeNext = false
              continue
            }
            
            if (char === "\\") {
              escapeNext = true
              continue
            }
            
            if (char === '"') {
              inString = !inString
              continue
            }
            
            if (!inString) {
              if (char === "{") braceCount++
              if (char === "}") {
                braceCount--
                if (braceCount === 0) {
                  jsonEnd = i + 1
                  break
                }
              }
            }
          }
          
          jsonText = trimmed.substring(0, jsonEnd)
        }
        
        // Parse the extracted JSON
        data = JSON.parse(jsonText)
        console.log(`[API] Parsed JSON successfully. Type: ${Array.isArray(data) ? 'Array' : typeof data}, Length: ${Array.isArray(data) ? data.length : 'N/A'}`)
      } else {
        console.warn(`[API] Response doesn't appear to be JSON. Content-Type: ${contentType}, starts with: ${trimmed.substring(0, 50)}`)
        // If it's an error response and not JSON, treat as plain text error message
        if (!res.ok) {
          data = { message: trimmed, error: trimmed }
        } else if (res.ok && trimmed.length < 5000) {
          // If it's a 200 response but not JSON, log the full response for debugging
          console.warn(`[API] Full non-JSON response:`, trimmed)
          data = null
        } else {
          data = null
        }
      }
    } catch (parseError) {
      console.error(`[API] JSON parse error:`, parseError.message)
      console.error(`[API] Response text (first 2000 chars):`, text.substring(0, 2000))
      if (res.ok) {
        console.error(`[API] Response was OK (${res.status}) but JSON parsing failed`)
        // Try to find if there's valid JSON somewhere in the response
        const jsonMatch = text.match(/(\[[\s\S]*\]|\{[\s\S]*\})/)
        if (jsonMatch) {
          try {
            data = JSON.parse(jsonMatch[0])
            console.log(`[API] Successfully extracted JSON from response`)
          } catch (e) {
            console.error(`[API] Failed to parse extracted JSON:`, e.message)
            data = null
          }
        } else {
          data = null
        }
      } else {
        // If it's an error response, preserve the text as error message
        if (!res.ok && text && text.trim().length > 0) {
          data = { message: text.trim(), error: text.trim() }
        } else {
          data = null
        }
      }
    }
  } else {
    console.log(`[API] Empty response body`)
    // If it's an error with empty body, still create error data
    if (!res.ok) {
      data = { message: `Request failed with status ${res.status}`, error: `Request failed with status ${res.status}` }
    } else {
      data = null
    }
  }

  if (!res.ok) {
    const message =
      (data && (data.message || data.error)) ||
      text?.trim() ||
      `Request failed with status ${res.status}`
    console.error(`[API] Request failed: ${message}`, { status: res.status, data })
    const error = new Error(message)
    error.status = res.status
    error.data = data || { message: text?.trim() || message, error: text?.trim() || message }
    throw error
  }

  console.log(`[API] Success, returning data:`, data ? (Array.isArray(data) ? `Array(${data.length})` : typeof data) : 'null')
  return data
}

// --- USER API ---

export const userAPI = {
  async register(username, email, password) {
    // Swagger: POST /auth/register { name, email, password }
    // Backend currently exposes /auth/register; if not, this will need to be aligned.
    return request("/auth/register", {
      method: "POST",
      body: { name: username, email, password },
    })
  },

  async login(loginOrEmail, password) {
    // Backend accepts login field which can be email or username
    const login = loginOrEmail
    if (!login || (typeof login === 'string' && login.trim() === "")) {
      console.error("[userAPI.login] Invalid login value:", { login, type: typeof login })
      throw new Error("Login cannot be empty")
    }
    if (!password || (typeof password === 'string' && password.trim() === "")) {
      console.error("[userAPI.login] Invalid password value:", { passwordLength: password?.length, type: typeof password })
      throw new Error("Password cannot be empty")
    }
    
    // Убеждаемся, что значения являются строками
    const loginStr = String(login).trim()
    const passwordStr = String(password)
    
    console.log("[userAPI.login] Attempting login with:", { 
      login: loginStr, 
      loginLength: loginStr.length,
      passwordLength: passwordStr?.length,
      originalLogin: login,
      originalPasswordType: typeof password
    })
    
    const requestBody = { login: loginStr, password: passwordStr }
    console.log("[userAPI.login] Request body object:", requestBody)
    
    const loginResponse = await request("/auth/login", {
      method: "POST",
      body: requestBody, // Используем login вместо email
    })

    console.log("[userAPI.login] Login response:", loginResponse)

    // Optionally hydrate user profile
    let user = null
    if (loginResponse && loginResponse.userId != null) {
      try {
        user = await this.getProfile(loginResponse.userId)
        console.log("[userAPI.login] Loaded user profile:", user)
      } catch (error) {
        console.warn("[userAPI.login] Failed to load user profile, using minimal user object:", error)
        user = { id: loginResponse.userId, email: loginOrEmail }
      }
    }

    const result = {
      id: loginResponse.userId,
      token: loginResponse.token,
      userId: loginResponse.userId,
      email: loginOrEmail,
      requiresTwoFactor: loginResponse.requiresTwoFactor || false,
      ...(user || {}),
    }
    console.log("[userAPI.login] Returning user object:", result)
    return result
  },

  async getProfile(id) {
    return request(`/users/${id}/profile`, { method: "GET" })
  },

  async updateInterests(userId, interests) {
    // PUT /users/{userId}/interests
    return request(`/users/${userId}/interests`, {
      method: "PUT",
      body: { interests },
    })
  },

  async getUserRooms(userId) {
    // Swagger: GET /users/{userId}/rooms
    return request(`/users/${userId}/rooms`, { method: "GET" })
  },

  async subscribe(userId, targetUserId) {
    // Use swagger-style profile actions: POST /users/{userId}/profile/view
    return request(`/users/${userId}/profile/view`, {
      method: "POST",
      body: {
        action: "addFriend",
        targetUserId,
      },
    })
  },

  async unsubscribe(userId, targetUserId) {
    // Backend doesn't expose explicit unsubscribe; we reuse the action endpoint
    // and let the server decide what to do. From UI standpoint we optimistically update.
    return request(`/users/${userId}/profile/view`, {
      method: "POST",
      body: {
        action: "block",
        targetUserId,
      },
    })
  },

  async getRating(userId) {
    // There is no explicit rating endpoint in swagger; return a mock structure to satisfy UI.
    return { rating: 8.5 }
  },

  async getRoomCount(userId) {
    // Backend: GET /users/{userId}/rooms/count
    return request(`/users/${userId}/rooms/count`, { method: "GET" })
  },

  async createRating(userId, raterUserId, score) {
    // Backend: POST /users/{userId}/ratings?raterUserId=
    // Score should be a number (0-10)
    const scoreValue = typeof score === 'number' ? score : parseFloat(score)
    if (isNaN(scoreValue) || scoreValue < 0 || scoreValue > 10) {
      throw new Error("Score must be between 0 and 10")
    }
    return request(`/users/${userId}/ratings`, {
      method: "POST",
      params: { raterUserId },
      body: { score: scoreValue },
    })
  },

  async getRatingSummary(userId) {
    // Backend: GET /users/{userId}/ratings/summary
    return request(`/users/${userId}/ratings/summary`, { method: "GET" })
  },

  async updateName(userId, nameRequest) {
    // Backend: PUT /users/{userId}/profile/personal-info/name
    return request(`/users/${userId}/profile/personal-info/name`, {
      method: "PUT",
      body: nameRequest,
    })
  },

  async updateAddress(userId, addressRequest) {
    // Backend: PUT /users/{userId}/profile/personal-info/address
    return request(`/users/${userId}/profile/personal-info/address`, {
      method: "PUT",
      body: addressRequest,
    })
  },

  async updateAbout(userId, aboutRequest) {
    // Backend: PUT /users/{userId}/about?currentUserId=
    return request(`/users/${userId}/about`, {
      method: "PUT",
      params: { currentUserId: userId },
      body: aboutRequest,
    })
  },

  async getAbout(userId) {
    // Backend: GET /users/{userId}/about
    return request(`/users/${userId}/about`, { method: "GET" })
  },

  async getNotificationSettings(userId) {
    // Backend: GET /users/{userId}/settings/general-notifications
    // UserController uses @RequestMapping("/users"), not "/api/users"
    return request(`/users/${userId}/settings/general-notifications`, { method: "GET" })
  },

  async updateNotificationSettings(userId, settings) {
    // Backend: PUT /users/{userId}/settings/general-notifications
    // settings should contain: { emailNotifications, pushNotifications }
    // UserController uses @RequestMapping("/users"), not "/api/users"
    return request(`/users/${userId}/settings/general-notifications`, {
      method: "PUT",
      body: settings,
    })
  },
}

// --- EXTERNAL LINKS API ---

export const externalLinksAPI = {
  async get(userId) {
    // Backend: GET /users/{userId}/profile/external-links
    return request(`/users/${userId}/profile/external-links`, { method: "GET" })
  },

  async create(userId, link) {
    // Backend: POST /users/{userId}/profile/external-links
    return request(`/users/${userId}/profile/external-links`, {
      method: "POST",
      body: link,
    })
  },

  async update(userId, linkId, link) {
    // Backend: PUT /users/{userId}/profile/external-links/{linkId}
    return request(`/users/${userId}/profile/external-links/${linkId}`, {
      method: "PUT",
      body: link,
    })
  },

  async delete(userId, linkId) {
    // Backend: DELETE /users/{userId}/profile/external-links/{linkId}
    return request(`/users/${userId}/profile/external-links/${linkId}`, {
      method: "DELETE",
    })
  },
}

// --- POSTS API ---

export const postAPI = {
  async create(post) {
    // Swagger: POST /posts with Post schema
    return request("/posts", {
      method: "POST",
      body: post,
    })
  },

  async like(userId, postId) {
    // Ensure IDs are primitive values
    const userIdValue = typeof userId === "object" ? (userId?.id || userId?.userId || null) : userId
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    
    if (!userIdValue || !postIdValue) {
      throw new Error(`Invalid IDs: userId=${userId}, postId=${postId}`)
    }
    
    // Backend: POST /posts/{postId}/like?userId=
    return request(`/posts/${postIdValue}/like`, {
      method: "POST",
      params: { userId: userIdValue },
    })
  },

  async dislike(userId, postId) {
    // Ensure IDs are primitive values
    const userIdValue = typeof userId === "object" ? (userId?.id || userId?.userId || null) : userId
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    
    if (!userIdValue || !postIdValue) {
      throw new Error(`Invalid IDs: userId=${userId}, postId=${postId}`)
    }
    
    // Backend: POST /posts/{postId}/dislike?userId=
    return request(`/posts/${postIdValue}/dislike`, {
      method: "POST",
      params: { userId: userIdValue },
    })
  },

  // Backend: GET /posts - get all posts
  async getAll() {
    return request("/posts", { method: "GET" })
  },

  // Backend: GET /posts/{postId} - get post by ID
  async getById(postId) {
    // Ensure postId is a primitive value
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    if (!postIdValue) {
      throw new Error(`Invalid postId: ${postId}`)
    }
    return request(`/posts/${postIdValue}`, { method: "GET" })
  },

  async getRecommended(userId) {
    // Backend: GET /posts/recommended?userId=&includeScores=true
    const response = await request("/posts/recommended", {
      method: "GET",
      params: { userId, includeScores: true },
    })
    
    console.log("[postAPI.getRecommended] Raw response:", response)
    
    // Проверяем различные форматы ответа
    let posts = []
    let scores = []
    let message = ""
    
    if (Array.isArray(response)) {
      // Если ответ - просто массив постов (старый формат)
      posts = response
      console.warn("[postAPI.getRecommended] Received array response (old format), no scores available")
    } else if (response && response.posts) {
      // Новый формат с scores
      posts = response.posts
      scores = response.similarityScores || []
      message = response.message || ""
    } else if (response) {
      // Возможно, response сам по себе массив постов
      posts = Array.isArray(response) ? response : [response]
    }
    
    // Выводим scores в консоль
    if (scores && scores.length > 0) {
      console.log("=".repeat(60))
      console.log("[postAPI.getRecommended] 📊 РЕКОМЕНДАЦИИ С SCORES")
      console.log("=".repeat(60))
      console.log(`[postAPI.getRecommended] Пользователь ID: ${userId}`)
      console.log(`[postAPI.getRecommended] Всего постов: ${posts.length}`)
      console.log(`[postAPI.getRecommended] Сообщение: ${message}`)
      console.log("-".repeat(60))
      
      // Выводим scores для каждого поста
      posts.forEach((post, index) => {
        if (index < scores.length) {
          const score = scores[index]
          console.log(
            `[${index + 1}] Post ID: ${post.id} | ` +
            `Score: ${score.toFixed(4)} | ` +
            `Title: "${post.name || 'No title'}"`
          )
        }
      })
      
      console.log("-".repeat(60))
      
      // Выводим статистику scores
      const maxScore = Math.max(...scores)
      const minScore = Math.min(...scores)
      const avgScore = scores.reduce((a, b) => a + b, 0) / scores.length
      console.log(`📈 Статистика scores:`)
      console.log(`   Максимум: ${maxScore.toFixed(4)}`)
      console.log(`   Минимум:  ${minScore.toFixed(4)}`)
      console.log(`   Среднее:  ${avgScore.toFixed(4)}`)
      console.log("=".repeat(60))
    } else {
      console.log("[postAPI.getRecommended] ⚠️ Scores не получены. Постов:", posts.length)
    }
    
    // Возвращаем только посты для обратной совместимости
    return posts
  },

  async getByUser(userId) {
    // Filter posts by author ID on client side for now
    // In future, backend could add GET /posts?authorId={userId}
    const allPosts = await this.getAll()
    return Array.isArray(allPosts) 
      ? allPosts.filter(post => post.author?.id === userId || post.authorId === userId)
      : []
  },

  async delete(postId, userId) {
    // Ensure IDs are primitive values
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    const userIdValue = typeof userId === "object" ? (userId?.id || userId?.userId || null) : userId
    
    if (!postIdValue || !userIdValue) {
      throw new Error(`Invalid IDs: postId=${postId}, userId=${userId}`)
    }
    
    // Backend: DELETE /posts/{postId}?userId=
    return request(`/posts/${postIdValue}`, {
      method: "DELETE",
      params: { userId: userIdValue },
    })
  },
}

// --- USER PROFILE API ---

export const profileAPI = {
  async getSubscriptions(userId) {
    // Get user profile which includes subscriptions
    const profile = await userAPI.getProfile(userId)
    return profile.subscriptions || []
  },

  async getFollowers(userId) {
    // Get user profile which includes followers
    const profile = await userAPI.getProfile(userId)
    return profile.followers || []
  },

  async uploadAvatar(userId, file) {
    // Backend: POST /users/{id}/avatar with multipart/form-data
    const formData = new FormData()
    formData.append("file", file)

    const url = new URL(`/users/${userId}/avatar`, API_BASE)
    const res = await fetch(url.toString(), {
      method: "POST",
      body: formData,
    })

    if (!res.ok) {
      const text = await res.text()
      let errorData = null
      try {
        errorData = text ? JSON.parse(text) : null
      } catch {
        // Ignore parse errors
      }
      const message =
        (errorData && (errorData.message || errorData.error)) ||
        `Request failed with status ${res.status}`
      const error = new Error(message)
      error.status = res.status
      error.data = errorData
      throw error
    }

    return res.json()
  },
}

// --- COMMENTS API ---

export const commentAPI = {
  async getByPost(postId) {
    // Ensure postId is a primitive value
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    if (!postIdValue) {
      throw new Error(`Invalid postId: ${postId}`)
    }
    // Backend: GET /posts/{postId}/comments
    return request(`/posts/${postIdValue}/comments`, { method: "GET" })
  },

  async create(postId, comment) {
    // Ensure postId is a primitive value
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    if (!postIdValue) {
      throw new Error(`Invalid postId: ${postId}`)
    }
    // Swagger: POST /posts/{postId}/comments
    return request(`/posts/${postIdValue}/comments`, {
      method: "POST",
      body: comment,
    })
  },

  async like(postId, commentId, userId) {
    // Ensure all IDs are primitive values
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    const commentIdValue = typeof commentId === "object" ? (commentId?.id || commentId?.commentId || null) : commentId
    const userIdValue = typeof userId === "object" ? (userId?.id || userId?.userId || null) : userId
    
    if (!postIdValue || !commentIdValue || !userIdValue) {
      throw new Error(`Invalid IDs: postId=${postId}, commentId=${commentId}, userId=${userId}`)
    }
    
    // Backend: POST /posts/{postId}/comments/{commentId}/like?userId=
    return request(`/posts/${postIdValue}/comments/${commentIdValue}/like`, {
      method: "POST",
      params: { userId: userIdValue },
    })
  },

  async dislike(postId, commentId, userId) {
    // Ensure all IDs are primitive values
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    const commentIdValue = typeof commentId === "object" ? (commentId?.id || commentId?.commentId || null) : commentId
    const userIdValue = typeof userId === "object" ? (userId?.id || userId?.userId || null) : userId
    
    if (!postIdValue || !commentIdValue || !userIdValue) {
      throw new Error(`Invalid IDs: postId=${postId}, commentId=${commentId}, userId=${userId}`)
    }
    
    // Backend: POST /posts/{postId}/comments/{commentId}/dislike?userId=
    return request(`/posts/${postIdValue}/comments/${commentIdValue}/dislike`, {
      method: "POST",
      params: { userId: userIdValue },
    })
  },

  async createReply(postId, parentCommentId, comment) {
    // Ensure postId is a primitive value
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    const parentCommentIdValue = typeof parentCommentId === "object" ? (parentCommentId?.id || parentCommentId?.commentId || null) : parentCommentId
    if (!postIdValue || !parentCommentIdValue) {
      throw new Error(`Invalid IDs: postId=${postId}, parentCommentId=${parentCommentId}`)
    }
    // Backend: POST /posts/{postId}/comments/{parentCommentId}/reply
    return request(`/posts/${postIdValue}/comments/${parentCommentIdValue}/reply`, {
      method: "POST",
      body: comment,
    })
  },

  async delete(postId, commentId, userId) {
    // Ensure all IDs are primitive values
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    const commentIdValue = typeof commentId === "object" ? (commentId?.id || commentId?.commentId || null) : commentId
    const userIdValue = typeof userId === "object" ? (userId?.id || userId?.userId || null) : userId
    
    if (!postIdValue || !commentIdValue || !userIdValue) {
      throw new Error(`Invalid IDs: postId=${postId}, commentId=${commentId}, userId=${userId}`)
    }
    
    // Backend: DELETE /posts/{postId}/comments/{commentId}?userId=
    return request(`/posts/${postIdValue}/comments/${commentIdValue}`, {
      method: "DELETE",
      params: { userId: userIdValue },
    })
  },
}

// --- ROOMS / CHAT API ---

export const roomAPI = {
  async create(userId, roomPayload) {
    // Backend: POST /api/rooms with description, category, maxCollaborators, meetingTime, meetingType, location
    return request("/api/rooms", {
      method: "POST",
      params: { userId },
      body: roomPayload,
    })
  },

  async getAllRooms(offset = 0, limit = 50) {
    // Backend: GET /api/rooms?offset=&limit=
    return request("/api/rooms", {
      method: "GET",
      params: { offset, limit },
    })
  },

  async getUserRooms(userId) {
    return userAPI.getUserRooms(userId)
  },

  async openChat(roomId, userId) {
    // Backend: GET /api/rooms/{roomId}/chat?userId=
    return request(`/api/rooms/${roomId}/chat`, {
      method: "GET",
      params: { userId },
    })
  },

  async sendMessage(roomId, senderId, content, imageId = null) {
    // Backend: POST /api/rooms/{roomId}/chat/messages with { senderId, content, imageId? }
    const body = {
      senderId,
      content,
    }
    if (imageId) {
      body.imageId = imageId
    }
    return request(`/api/rooms/${roomId}/chat/messages`, {
      method: "POST",
      body,
    })
  },

  async search(query, filters = {}) {
    // Backend: GET /api/rooms/search?query=&category=&startDate=&endDate=&location=
    const params = {}
    if (query) params.query = query
    if (filters.category) params.category = filters.category
    if (filters.startDate) params.startDate = filters.startDate
    if (filters.endDate) params.endDate = filters.endDate
    if (filters.location) params.location = filters.location
    
    return request("/api/rooms/search", {
      method: "GET",
      params,
    })
  },

  async applyToRoom(roomId, userId) {
    // Backend: POST /api/rooms/{roomId}/apply
    return request(`/api/rooms/${roomId}/apply`, {
      method: "POST",
      body: { userId },
    })
  },

  async getPendingJoinRequests(roomId) {
    // Backend: GET /api/rooms/{roomId}/join-requests
    return request(`/api/rooms/${roomId}/join-requests`, {
      method: "GET",
    })
  },

  /**
   * Get all pending requests for the current user (applicant view)
   * GET /api/rooms/my-applications?userId={userId}
   * 
   * @param {number} userId - The user ID
   * @returns {Promise<Array>} List of user's pending RoomJoinRequest
   */
  async getMyPendingRequests(userId) {
    // Backend: GET /api/rooms/my-applications?userId={userId}
    return request("/api/rooms/my-applications", {
      method: "GET",
      params: { userId },
    })
  },

  async getDetails(roomId) {
    // Backend: GET /api/rooms/{roomId}
    return request(`/api/rooms/${roomId}`, { method: "GET" })
  },

  /**
   * Create a new room join request
   * POST /api/rooms/{roomId}/requests?userId={userId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} userId - The user ID requesting to join
   * @param {Object} requestBody - Optional request with message (max 500 chars)
   * @param {string} [requestBody.message] - Optional message from requester
   * @returns {Promise<Object>} Created RoomJoinRequest
   * @throws {Error} If room is "open" type, user already member, cooldown active, etc.
   */
  async createJoinRequest(roomId, userId, requestBody = {}) {
    // Backend: POST /api/rooms/{roomId}/requests?userId={userId}
    return request(`/api/rooms/${roomId}/requests`, {
      method: "POST",
      params: { userId },
      body: requestBody,
    })
  },

  /**
   * Get all pending requests for a room (admin/creator view)
   * GET /api/rooms/{roomId}/requests?userId={userId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} userId - The admin/creator user ID
   * @returns {Promise<Array>} List of RoomJoinRequest
   * @throws {Error} If user is not admin/creator (403)
   */
  async getPendingRequests(roomId, userId) {
    // Backend: GET /api/rooms/{roomId}/requests?userId={userId}
    return request(`/api/rooms/${roomId}/requests`, {
      method: "GET",
      params: { userId },
    })
  },

  /**
   * Approve a room join request
   * POST /api/rooms/{roomId}/requests/{requestId}/approve?targetUserId={targetUserId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} requestId - The request ID
   * @param {number} targetUserId - The user ID whose request is being approved
   * @param {Object} approveRequest - Request with adminId
   * @param {number} approveRequest.adminId - ID of admin approving
   * @returns {Promise<void>}
   * @throws {Error} If not admin (403), room at capacity (409), etc.
   */
  async approveRequest(roomId, requestId, targetUserId, approveRequest) {
    // Backend: POST /api/rooms/{roomId}/requests/{requestId}/approve?targetUserId={targetUserId}
    return request(`/api/rooms/${roomId}/requests/${requestId}/approve`, {
      method: "POST",
      params: { targetUserId },
      body: approveRequest,
    })
  },

  /**
   * Reject a room join request
   * POST /api/rooms/{roomId}/requests/{requestId}/reject?targetUserId={targetUserId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} requestId - The request ID
   * @param {number} targetUserId - The user ID whose request is being rejected
   * @param {Object} rejectRequest - Request with adminId and optional reason
   * @param {number} rejectRequest.adminId - ID of admin rejecting
   * @param {string} [rejectRequest.reason] - Optional rejection reason (max 500 chars)
   * @returns {Promise<void>}
   * @throws {Error} If not admin (403), request not pending (400), etc.
   */
  async rejectRequest(roomId, requestId, targetUserId, rejectRequest) {
    // Backend: POST /api/rooms/{roomId}/requests/{requestId}/reject?targetUserId={targetUserId}
    return request(`/api/rooms/${roomId}/requests/${requestId}/reject`, {
      method: "POST",
      params: { targetUserId },
      body: rejectRequest,
    })
  },

  /**
   * Cancel a room join request (user cancels their own request)
   * DELETE /api/rooms/{roomId}/requests/{requestId}?userId={userId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} requestId - The request ID
   * @param {number} userId - The user ID (must be request owner)
   * @returns {Promise<void>}
   * @throws {Error} If not request owner (403), request not pending (400), etc.
   */
  async cancelRequest(roomId, requestId, userId) {
    // Backend: DELETE /api/rooms/{roomId}/requests/{requestId}?userId={userId}
    return request(`/api/rooms/${roomId}/requests/${requestId}`, {
      method: "DELETE",
      params: { userId },
    })
  },

  /**
   * Manually close a room
   * POST /api/rooms/{roomId}/close
   * 
   * @param {number} roomId - The room ID
   * @param {Object} closeRequest - Request with userId
   * @param {number} closeRequest.userId - ID of user closing (must be admin/creator)
   * @returns {Promise<void>}
   * @throws {Error} If not admin (403), room already closed (400), etc.
   */
  async closeRoom(roomId, closeRequest) {
    // Backend: POST /api/rooms/{roomId}/close
    return request(`/api/rooms/${roomId}/close`, {
      method: "POST",
      body: closeRequest,
    })
  },

  // ========== Legacy Methods (deprecated but maintained for backward compatibility) ==========

  /**
   * @deprecated Use createJoinRequest instead
   */
  async createMembershipRequest(roomId, userId, message) {
    // Backend: POST /api/rooms/{roomId}/requests?userId={userId}
    return this.createJoinRequest(roomId, userId, message ? { message } : {})
  },

  /**
   * @deprecated Use getPendingRequests instead
   */
  async getMembershipRequests(roomId, userId) {
    return this.getPendingRequests(roomId, userId)
  },

  /**
   * @deprecated Use cancelRequest instead
   */
  async cancelMembershipRequest(roomId, requestId, userId) {
    return this.cancelRequest(roomId, requestId, userId)
  },

  async pinPost(roomId, postId, userId) {
    // Backend: POST /api/rooms/{roomId}/pinned-posts?userId=
    return request(`/api/rooms/${roomId}/pinned-posts`, {
      method: "POST",
      params: { userId },
      body: { postId },
    })
  },

  async unpinPost(roomId, postId, userId) {
    // Backend: DELETE /api/rooms/{roomId}/pinned-posts/{postId}?userId=
    return request(`/api/rooms/${roomId}/pinned-posts/${postId}`, {
      method: "DELETE",
      params: { userId },
    })
  },

  async getPinnedPosts(roomId) {
    // Backend: GET /api/rooms/{roomId}/pinned-posts
    return request(`/api/rooms/${roomId}/pinned-posts`, { method: "GET" })
  },

  async joinRoom(roomId, userId) {
    // Backend: POST /rooms/{roomId}/join
    return request(`/api/rooms/${roomId}/join`, {
      method: "POST",
      body: { userId },
    })
  },

  async createRatingRequest(roomId, requestedUserId, requesterUserId) {
    // Backend: POST /api/rooms/{roomId}/rating-requests?requestedUserId=&requesterUserId=
    return request(`/api/rooms/${roomId}/rating-requests`, {
      method: "POST",
      params: { requestedUserId, requesterUserId },
    })
  },

  /**
   * Kick a user from a room (admin/creator only)
   * POST /api/rooms/{roomId}/admin/kick
   * 
   * @param {number} roomId - The room ID
   * @param {number} userIdToKick - The user ID to kick
   * @param {number} adminUserId - The admin/creator user ID
   * @returns {Promise<void>}
   * @throws {Error} If not admin (403), user not found (404), etc.
   */
  async kickUserFromRoom(roomId, userIdToKick, adminUserId) {
    // Backend: POST /api/rooms/{roomId}/admin/kick?userIdToKick=&adminUserId=
    return request(`/api/rooms/${roomId}/admin/kick`, {
      method: "POST",
      params: { userIdToKick, adminUserId },
    })
  },

  /**
   * Delete a message from room chat (admin/creator only)
   * DELETE /api/rooms/{roomId}/chat/messages/{messageId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} messageId - The message ID
   * @param {number} adminUserId - The admin/creator user ID
   * @returns {Promise<void>}
   * @throws {Error} If not admin (403), message not found (404), etc.
   */
  async deleteMessage(roomId, messageId, adminUserId) {
    // Backend: DELETE /api/rooms/{roomId}/chat/messages/{messageId}?adminUserId=
    return request(`/api/rooms/${roomId}/chat/messages/${messageId}`, {
      method: "DELETE",
      params: { adminUserId },
    })
  },

  /**
   * Promote a user to admin in a room (creator only)
   * POST /api/rooms/{roomId}/admin/promote
   * 
   * @param {number} roomId - The room ID
   * @param {number} userIdToPromote - The user ID to promote
   * @param {number} adminUserId - The creator user ID
   * @returns {Promise<void>}
   * @throws {Error} If not creator (403), user not found (404), etc.
   */
  async promoteToAdmin(roomId, userIdToPromote, adminUserId) {
    // Backend: POST /api/rooms/{roomId}/admin/promote?userIdToPromote=&adminUserId=
    return request(`/api/rooms/${roomId}/admin/promote`, {
      method: "POST",
      params: { userIdToPromote, adminUserId },
    })
  },

  /**
   * Demote a user from admin in a room (creator only)
   * POST /api/rooms/{roomId}/admin/demote
   * 
   * @param {number} roomId - The room ID
   * @param {number} userIdToDemote - The user ID to demote
   * @param {number} adminUserId - The creator user ID
   * @returns {Promise<void>}
   * @throws {Error} If not creator (403), user not found (404), etc.
   */
  async demoteFromAdmin(roomId, userIdToDemote, adminUserId) {
    // Backend: POST /api/rooms/{roomId}/admin/demote?userIdToDemote=&adminUserId=
    return request(`/api/rooms/${roomId}/admin/demote`, {
      method: "POST",
      params: { userIdToDemote, adminUserId },
    })
  },
}

// --- NOTIFICATIONS API ---

export const notificationAPI = {
  async getAll(userId) {
    // Backend: GET /api/notifications/{userId}
    return request(`/api/notifications/${userId}`, {
      method: "GET",
    })
  },

  async getUnread(userId) {
    // Backend: GET /api/notifications/{userId}/unread
    return request(`/api/notifications/${userId}/unread`, {
      method: "GET",
    })
  },

  async getUnreadCount(userId) {
    // Backend: GET /api/notifications/{userId}/unread-count
    return request(`/api/notifications/${userId}/unread-count`, {
      method: "GET",
    })
  },

  async markAsRead(notificationId, userId) {
    // Backend: POST /api/notifications/{notificationId}/read?userId=
    return request(`/api/notifications/${notificationId}/read`, {
      method: "POST",
      params: { userId },
    })
  },

  async markAllAsRead(userId) {
    // Backend: POST /api/notifications/{userId}/read-all
    return request(`/api/notifications/${userId}/read-all`, {
      method: "POST",
    })
  },

  async markNotificationsAsRead(notificationIds, userId) {
    // Backend: POST /api/notifications/mark-read?userId=
    return request("/api/notifications/mark-read", {
      method: "POST",
      params: { userId },
      body: notificationIds,
    })
  },

  async dismiss(notificationId, userId) {
    // Backend: DELETE /api/notifications/{notificationId}?userId=
    return request(`/api/notifications/${notificationId}`, {
      method: "DELETE",
      params: { userId },
    })
  },
}

// --- IMAGES API ---

export const imageAPI = {
  async upload(file) {
    // Backend: POST /images/upload with multipart/form-data
    const formData = new FormData()
    formData.append("file", file)

    const url = new URL("/images/upload", API_BASE)
    const res = await fetch(url.toString(), {
      method: "POST",
      body: formData,
      // Не устанавливаем Content-Type явно - браузер установит его автоматически с boundary для multipart/form-data
    })

    if (!res.ok) {
      const text = await res.text()
      let errorData = null
      try {
        errorData = text ? JSON.parse(text) : null
      } catch {
        // Ignore parse errors
      }
      const message =
        (errorData && (errorData.message || errorData.error)) ||
        `Request failed with status ${res.status}`
      const error = new Error(message)
      error.status = res.status
      error.data = errorData
      throw error
    }

    return res.json()
  },

  getImageUrl(imageId) {
    // Backend: GET /images/{id} returns image bytes
    return `${API_BASE}/images/${imageId}`
  },
}

// --- PASSWORD RESET API ---

export const passwordResetAPI = {
  async request(email) {
    // Backend: POST /password-reset/request
    return request("/password-reset/request", {
      method: "POST",
      body: { email },
    })
  },

  async confirm(token, newPassword) {
    // Backend: POST /password-reset/confirm
    return request("/password-reset/confirm", {
      method: "POST",
      body: { token, newPassword },
    })
  },

  async validateToken(token) {
    // Backend: GET /password-reset/validate-token?token=
    return request("/password-reset/validate-token", {
      method: "GET",
      params: { token },
    })
  },
}

// --- EMAIL VERIFICATION API ---

export const emailVerificationAPI = {
  async verify(token) {
    // Backend: POST /email-verification/verify?token=
    return request("/email-verification/verify", {
      method: "POST",
      params: { token },
    })
  },

  async validateToken(token) {
    // Backend: GET /email-verification/validate-token?token=
    return request("/email-verification/validate-token", {
      method: "GET",
      params: { token },
    })
  },

  async resend(email) {
    // Backend: POST /email-verification/resend?email=
    return request("/email-verification/resend", {
      method: "POST",
      params: { email },
    })
  },
}

// --- TWO FACTOR AUTHENTICATION API ---

export const twoFactorAPI = {
  async enable(userId) {
    // Backend: POST /auth/2fa/enable/{userId}
    return request(`/auth/2fa/enable/${userId}`, {
      method: "POST",
    })
  },

  async verifySetup(userId, code, secret) {
    // Backend: POST /auth/2fa/verify-setup/{userId}?secret=
    return request(`/auth/2fa/verify-setup/${userId}`, {
      method: "POST",
      params: { secret },
      body: { code },
    })
  },

  async disable(userId) {
    // Backend: POST /auth/2fa/disable/{userId}
    return request(`/auth/2fa/disable/${userId}`, {
      method: "POST",
    })
  },

  async getStatus(userId) {
    // Backend: GET /auth/2fa/status/{userId}
    return request(`/auth/2fa/status/${userId}`, {
      method: "GET",
    })
  },

  async loginWithTwoFactor(email, password, code) {
    // Backend: POST /auth/login with 2FA code in body
    // This is called after initial login returns requiresTwoFactor: true
    return request("/auth/login", {
      method: "POST",
      body: { email, password, twoFactorCode: code },
    })
  },
}


