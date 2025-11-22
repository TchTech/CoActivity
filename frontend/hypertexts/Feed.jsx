"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { getAllPosts, getPostsByUserIds } from "../scripts/postsData"
import { postAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/post.css"
import "../styles/navigation.css"

/**
 * Компонент ленты с постами
 * Имеет две вкладки: Главное (все посты) и Мои подписки (посты от подписок)
 */
function Feed({ onNavigate }) {
  const { currentUser, subscribedUsers, subscribeToUser, unsubscribeFromUser } = useUser()
  const [activeTab, setActiveTab] = useState("main") // 'main' или 'subscriptions'
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [postLikes, setPostLikes] = useState({})
  const [postDislikes, setPostDislikes] = useState({})
  const [isLiked, setIsLiked] = useState({})
  const [isDisliked, setIsDisliked] = useState({})

  // Загрузка постов
  useEffect(() => {
    const loadPosts = async () => {
      setLoading(true)
      try {
        // TODO: Когда будет добавлен GET /posts эндпоинт в backend
        // const allPosts = await postAPI.getAll()
        // if (activeTab === "main") {
        //   setPosts(allPosts)
        // } else {
        //   // Получить посты от подписок
        //   const subscriptionPosts = await Promise.all(
        //     subscribedUsers.map((userId) => postAPI.getByUser(userId))
        //   )
        //   setPosts(subscriptionPosts.flat())
        // }
        
        // Временно используем моковые данные
        const allPosts = activeTab === "main" ? getAllPosts() : getPostsByUserIds(subscribedUsers.map(u => u.id || u))
        setPosts(allPosts)
      } catch (error) {
        console.error("Ошибка загрузки постов:", error)
        // Fallback на моковые данные
        const allPosts = activeTab === "main" ? getAllPosts() : getPostsByUserIds(subscribedUsers.map(u => u.id || u))
        setPosts(allPosts)
      } finally {
        setLoading(false)
      }
    }

    loadPosts()
  }, [activeTab, subscribedUsers])

  // Инициализация состояний для постов
  useEffect(() => {
    const initialLikes = {}
    const initialDislikes = {}
    posts.forEach((post) => {
      initialLikes[post.id] = post.likes || 0
      initialDislikes[post.id] = post.dislikes || 0
    })
    setPostLikes(initialLikes)
    setPostDislikes(initialDislikes)
  }, [posts])

  const handlePostLike = async (postId) => {
    if (!currentUser) {
      console.warn("Пользователь не авторизован")
      return
    }

    const wasLiked = isLiked[postId]
    const wasDisliked = isDisliked[postId]

    try {
      // Вызов API для лайка
      await postAPI.like(currentUser.id, postId)

      // Оптимистичное обновление UI
      if (wasLiked) {
        setPostLikes((prev) => ({ ...prev, [postId]: (prev[postId] || 0) - 1 }))
        setIsLiked((prev) => ({ ...prev, [postId]: false }))
      } else {
        setPostLikes((prev) => ({ ...prev, [postId]: (prev[postId] || 0) + 1 }))
        setIsLiked((prev) => ({ ...prev, [postId]: true }))
        
        if (wasDisliked) {
          setPostDislikes((prev) => ({ ...prev, [postId]: (prev[postId] || 0) - 1 }))
          setIsDisliked((prev) => ({ ...prev, [postId]: false }))
        }
      }
    } catch (error) {
      console.error("Ошибка при лайке поста:", error)
      // Можно показать уведомление об ошибке
    }
  }

  const handlePostDislike = async (postId) => {
    if (!currentUser) {
      console.warn("Пользователь не авторизован")
      return
    }

    const wasDisliked = isDisliked[postId]
    const wasLiked = isLiked[postId]

    try {
      // Вызов API для дизлайка
      await postAPI.dislike(currentUser.id, postId)

      // Оптимистичное обновление UI
      if (wasDisliked) {
        setPostDislikes((prev) => ({ ...prev, [postId]: (prev[postId] || 0) - 1 }))
        setIsDisliked((prev) => ({ ...prev, [postId]: false }))
      } else {
        setPostDislikes((prev) => ({ ...prev, [postId]: (prev[postId] || 0) + 1 }))
        setIsDisliked((prev) => ({ ...prev, [postId]: true }))
        
        if (wasLiked) {
          setPostLikes((prev) => ({ ...prev, [postId]: (prev[postId] || 0) - 1 }))
          setIsLiked((prev) => ({ ...prev, [postId]: false }))
        }
      }
    } catch (error) {
      console.error("Ошибка при дизлайке поста:", error)
    }
  }

  const handleSubscribe = async (userId) => {
    if (!currentUser) {
      console.warn("Пользователь не авторизован")
      return
    }

    try {
      const isSubscribed = subscribedUsers.some((u) => (u.id || u) === userId)
      if (isSubscribed) {
        await unsubscribeFromUser(currentUser.id, userId)
      } else {
        await subscribeToUser(currentUser.id, userId)
      }
    } catch (error) {
      console.error("Ошибка при подписке/отписке:", error)
    }
  }

  return (
    <div>
      <div className="top-nav">
        <div className="top-nav-title">CoActivity</div>
        <button className="btn-icon" onClick={() => onNavigate("settings")}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 1v6m0 6v6m-6-6h6m6 0h-6m-5.6-5.6l4.2 4.2m4.2 4.2l4.2 4.2m-4.2 0l4.2-4.2M5.6 5.6l4.2 4.2" />
          </svg>
        </button>
      </div>

      {/* Вкладки */}
      <div className="tabs">
        <button className={`tab ${activeTab === "main" ? "active" : ""}`} onClick={() => setActiveTab("main")}>
          Главное
        </button>
        <button
          className={`tab ${activeTab === "subscriptions" ? "active" : ""}`}
          onClick={() => setActiveTab("subscriptions")}
        >
          Мои подписки
        </button>
      </div>

      {/* Лента постов */}
      <div style={{ padding: "var(--spacing-md)", paddingBottom: "80px" }}>
        {loading ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            Загрузка...
          </div>
        ) : posts.length === 0 ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            {activeTab === "subscriptions" ? "Нет постов от ваших подписок" : "Нет постов"}
          </div>
        ) : (
          posts.map((post) => (
            <div key={post.id} className="post-card" style={{ marginBottom: "var(--spacing-md)" }}>
              <div className="post-header">
                <img
                  src={post.author.avatar || "/placeholder.svg"}
                  alt={post.author.name}
                  className="avatar avatar-md avatar-clickable"
                  onClick={(e) => {
                    e.stopPropagation()
                    onNavigate("profile", post.userId)
                  }}
                />
                <div className="post-user-info">
                  <div className="post-username">
                    {post.author.name}
                    <span className="badge badge-rating">{post.author.rating}</span>
                  </div>
                  <div className="post-time">{post.time}</div>
                </div>
                <button
                  className={`btn ${subscribedUsers.some((u) => (u.id || u) === post.userId) ? "btn-secondary" : "btn-primary"}`}
                  onClick={(e) => {
                    e.stopPropagation()
                    handleSubscribe(post.userId)
                  }}
                >
                  {subscribedUsers.some((u) => (u.id || u) === post.userId) ? "отписаться" : "подписаться"}
                </button>
              </div>

              <h3 className="post-title">{post.title}</h3>
              <p className="post-content">{post.content}</p>

              <img
                src={post.image || "/placeholder.svg"}
                alt={post.title}
                className="post-image"
                onClick={() => onNavigate("comments", post.id)}
                style={{ cursor: "pointer" }}
              />

              <div className="post-actions">
                <button 
                  className={`post-action-btn ${isLiked[post.id] ? "liked" : ""}`} 
                  onClick={() => handlePostLike(post.id)}
                >
                  <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M7 22V11M2 13l5-10 5 10M17 22v-6M12 18l5-6 5 6" fill={isLiked[post.id] ? "currentColor" : "none"}/>
                    <path d="M12 2L7 7h10L12 2z" fill={isLiked[post.id] ? "currentColor" : "none"}/>
                    <path d="M7 7v15h10V7" fill={isLiked[post.id] ? "currentColor" : "none"}/>
                  </svg>
                  <span>{postLikes[post.id] !== undefined ? postLikes[post.id] : post.likes}</span>
                </button>
                <button 
                  className={`post-action-btn ${isDisliked[post.id] ? "disliked" : ""}`} 
                  onClick={() => handlePostDislike(post.id)}
                >
                  <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17 2v11M22 11l-5-10-5 10M7 2v6M12 6l-5 6-5-6" fill={isDisliked[post.id] ? "currentColor" : "none"}/>
                    <path d="M12 22L7 17h10L12 22z" fill={isDisliked[post.id] ? "currentColor" : "none"}/>
                    <path d="M7 17V2h10v15" fill={isDisliked[post.id] ? "currentColor" : "none"}/>
                  </svg>
                  <span>{postDislikes[post.id] !== undefined ? postDislikes[post.id] : post.dislikes || 0}</span>
                </button>
                <button className="post-action-btn" onClick={() => onNavigate("comments", post.id)}>
                  <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                  <span>{post.comments}</span>
                </button>
                <button
                  className="post-action-btn"
                  style={{
                    marginLeft: "auto",
                    backgroundColor: "var(--accent-gold)",
                    color: "var(--bg-primary)",
                    padding: "8px 20px",
                    borderRadius: "var(--radius-full)",
                  }}
                  onClick={() => console.log("[v0] Откликнуться на пост:", post.id)}
                >
                  ОТКЛИКНУТЬСЯ
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      <BottomNavigation currentPage="home" onNavigate={onNavigate} />
    </div>
  )
}

export default Feed
