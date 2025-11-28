// Simple API helper for the CoActivity backend
// Uses fetch and maps to the existing Spring Boot endpoints.

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ||
  (typeof window !== "undefined" && window.location.origin.includes("localhost")
    ? "http://localhost:8080"
    : "http://localhost:8080")

async function request(path, { method = "GET", params, body, headers } = {}) {
  const url = new URL(path, API_BASE)

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.append(key, String(value))
      }
    })
  }

  const options = {
    method,
    headers: {
      ...(body && !(body instanceof FormData)
        ? { "Content-Type": "application/json" }
        : {}),
      ...headers,
    },
    body:
      body && body instanceof FormData
        ? body
        : body
        ? JSON.stringify(body)
        : undefined,
  }

  const res = await fetch(url.toString(), options)

  // Some endpoints legitimately return empty bodies
  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  if (!res.ok) {
    const message =
      (data && (data.message || data.error)) ||
      `Request failed with status ${res.status}`
    const error = new Error(message)
    error.status = res.status
    error.data = data
    throw error
  }

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
    // Swagger: POST /auth/login { email, password }
    const email = loginOrEmail
    const loginResponse = await request("/auth/login", {
      method: "POST",
      body: { email, password },
    })

    // Optionally hydrate user profile
    let user = null
    if (loginResponse && loginResponse.userId != null) {
      try {
        user = await this.getProfile(loginResponse.userId)
      } catch {
        user = { id: loginResponse.userId, email }
      }
    }

    return {
      token: loginResponse.token,
      userId: loginResponse.userId,
      ...(user || {}),
    }
  },

  async getProfile(id) {
    return request(`/users/${id}/profile`, { method: "GET" })
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
    // Backend: POST /posts/{postId}/like?userId=
    return request(`/posts/${postId}/like`, {
      method: "POST",
      params: { userId },
    })
  },

  async dislike(userId, postId) {
    // Backend: POST /posts/{postId}/dislike?userId=
    return request(`/posts/${postId}/dislike`, {
      method: "POST",
      params: { userId },
    })
  },

  // Backend: GET /posts - get all posts
  async getAll() {
    return request("/posts", { method: "GET" })
  },

  // Backend: GET /posts/{postId} - get post by ID
  async getById(postId) {
    return request(`/posts/${postId}`, { method: "GET" })
  },

  async getByUser(userId) {
    // Filter posts by author ID on client side for now
    // In future, backend could add GET /posts?authorId={userId}
    const allPosts = await this.getAll()
    return Array.isArray(allPosts) 
      ? allPosts.filter(post => post.author?.id === userId || post.authorId === userId)
      : []
  },
}

// --- COMMENTS API ---

export const commentAPI = {
  async getByPost(postId) {
    // Backend currently does not expose GET /posts/{postId}/comments.
    // This will throw unless such endpoint is added; callers are expected
    // to fall back to local mock data on error.
    return request(`/posts/${postId}/comments`, { method: "GET" })
  },

  async create(postId, comment) {
    // Swagger: POST /posts/{postId}/comments
    return request(`/posts/${postId}/comments`, {
      method: "POST",
      body: comment,
    })
  },

  async like(postId, commentId, userId) {
    // Backend: POST /posts/{postId}/comments/{commentId}/like?userId=
    return request(`/posts/${postId}/comments/${commentId}/like`, {
      method: "POST",
      params: { userId },
    })
  },
}

// --- ROOMS / CHAT API ---

export const roomAPI = {
  async create(userId, roomPayload) {
    // Swagger: POST /rooms with description, category, maxCollaborators, meetingTime, meetingType, location
    return request("/rooms", {
      method: "POST",
      params: { userId },
      body: roomPayload,
    })
  },

  async getUserRooms(userId) {
    return userAPI.getUserRooms(userId)
  },

  async openChat(roomId, userId) {
    // Backend: GET /rooms/{roomId}/chat?userId=
    return request(`/rooms/${roomId}/chat`, {
      method: "GET",
      params: { userId },
    })
  },

  async sendMessage(roomId, senderId, content) {
    // Swagger: POST /rooms/{roomId}/chat/messages with { senderId, content }
    return request(`/rooms/${roomId}/chat/messages`, {
      method: "POST",
      body: {
        senderId,
        content,
      },
    })
  },
}


