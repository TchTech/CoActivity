"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { getAllPosts, getPostsByUserIds } from "../scripts/postsData"
import { postAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { usePostInteractions } from "../hooks/usePostInteractions"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/post.css"
import "../styles/navigation.css"

/**
 * Компонент ленты с постами
 * Имеет две вкладки: Главное (все посты) и Мои подписки (посты от подписок)
 */
function FeedPostCard({ post, onNavigate, subscribedUsers, handleSubscribe }) {
  // Safe: hook is called at the top level of this component,
  // not inside a loop in another component.
  const postInteractions = usePostInteractions(post)

  return (
    <div key={post.id} className="post-card" style={{ marginBottom: "var(--spacing-md)" }}>
      <div className="post-header">
        <img
          src={post.author?.avatar || "/placeholder.svg"}
          alt={post.author?.name || "Пользователь"}
          className="avatar avatar-md avatar-clickable"
          onClick={(e) => {
            e.stopPropagation()
            const userId = post.userId || post.author?.id
            if (userId) {
              onNavigate("profile", userId)
            }
          }}
        />
        <div className="post-user-info">
          <div className="post-username">
            {post.author?.name || post.author?.username || "Пользователь"}
            {post.author?.rating && (
              <span className="badge badge-rating">{post.author.rating.toFixed(1)}</span>
            )}
          </div>
          <div className="post-time">
            {post.time ||
              (post.createdAt ? new Date(post.createdAt).toLocaleDateString("ru-RU") : "")}
          </div>
        </div>
        <button
          className={`btn ${
            subscribedUsers.some(
              (u) => (u.id || u) === (post.userId || post.author?.id)
            )
              ? "btn-secondary"
              : "btn-primary"
          }`}
          onClick={(e) => {
            e.stopPropagation()
            handleSubscribe(post.userId || post.author?.id)
          }}
        >
          {subscribedUsers.some((u) => (u.id || u) === (post.userId || post.author?.id))
            ? "отписаться"
            : "подписаться"}
        </button>
      </div>

      <h3 className="post-title">{post.name || post.title}</h3>
      <p className="post-content">{post.text || post.content}</p>

      {post.image && (
        <img
          src={
            typeof post.image === "object" && post.image.id
              ? imageAPI.getImageUrl(post.image.id)
              : post.image?.url || post.image || "/placeholder.svg"
          }
          alt={post.name || post.title}
          className="post-image"
          onClick={() => {
            const postId = typeof post.id === "object" ? (post.id?.id || post.id?.postId || null) : post.id
            if (postId) {
              onNavigate("comments", postId)
            } else {
              console.error("[Feed] Invalid post.id:", post.id)
            }
          }}
          style={{ cursor: "pointer" }}
        />
      )}

      {post.externalLinks && (
        <div className="post-external-links" style={{ marginTop: "var(--spacing-sm)", marginBottom: "var(--spacing-sm)" }}>
          {(() => {
            try {
              const links = typeof post.externalLinks === "string" 
                ? JSON.parse(post.externalLinks) 
                : post.externalLinks
              if (Array.isArray(links) && links.length > 0) {
                return (
                  <div>
                    <strong style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)" }}>Ссылки:</strong>
                    {links.map((link, idx) => (
                      <a
                        key={idx}
                        href={link}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{
                          display: "block",
                          color: "var(--accent-blue)",
                          textDecoration: "underline",
                          marginTop: "var(--spacing-xs)",
                          fontSize: "var(--font-size-sm)",
                        }}
                        onClick={(e) => e.stopPropagation()}
                      >
                        {link}
                      </a>
                    ))}
                  </div>
                )
              }
            } catch (e) {
              // If parsing fails, try to display as plain text
              return (
                <div>
                  <strong style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)" }}>Ссылка:</strong>
                  <a
                    href={post.externalLinks}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{
                      display: "block",
                      color: "var(--accent-blue)",
                      textDecoration: "underline",
                      marginTop: "var(--spacing-xs)",
                      fontSize: "var(--font-size-sm)",
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    {post.externalLinks}
                  </a>
                </div>
              )
            }
            return null
          })()}
        </div>
      )}

      <div className="post-actions">
        <button
          className={`post-action-btn ${postInteractions.isLiked ? "liked" : ""}`}
          onClick={(e) => {
            e.stopPropagation()
            postInteractions.handleLike()
          }}
          disabled={postInteractions.loading}
        >
          <svg
            className="post-action-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path
              d="M7 22V11M2 13l5-10 5 10M17 22v-6M12 18l5-6 5 6"
              fill={postInteractions.isLiked ? "currentColor" : "none"}
            />
            <path
              d="M12 2L7 7h10L12 2z"
              fill={postInteractions.isLiked ? "currentColor" : "none"}
            />
            <path
              d="M7 7v15h10V7"
              fill={postInteractions.isLiked ? "currentColor" : "none"}
            />
          </svg>
          <span>{postInteractions.likes}</span>
        </button>
        <button
          className={`post-action-btn ${postInteractions.isDisliked ? "disliked" : ""}`}
          onClick={(e) => {
            e.stopPropagation()
            postInteractions.handleDislike()
          }}
          disabled={postInteractions.loading}
        >
          <svg
            className="post-action-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path
              d="M17 2v11M22 11l-5-10-5 10M7 2v6M12 6l-5 6-5-6"
              fill={postInteractions.isDisliked ? "currentColor" : "none"}
            />
            <path
              d="M12 22L7 17h10L12 22z"
              fill={postInteractions.isDisliked ? "currentColor" : "none"}
            />
            <path
              d="M7 17V2h10v15"
              fill={postInteractions.isDisliked ? "currentColor" : "none"}
            />
          </svg>
          <span>{postInteractions.dislikes}</span>
        </button>
        <button
          className="post-action-btn"
          onClick={() => {
            const postId = typeof post.id === "object" ? (post.id?.id || post.id?.postId || null) : post.id
            if (postId) {
              onNavigate("comments", postId)
            } else {
              console.error("[Feed] Invalid post.id:", post.id)
            }
          }}
        >
          <svg
            className="post-action-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
          >
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <span>{post.comments?.length || post.comments || 0}</span>
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
  )
}

function Feed({ onNavigate }) {
  const { currentUser, subscribedUsers, subscribeToUser, unsubscribeFromUser } = useUser()
  const [activeTab, setActiveTab] = useState("main") // 'main' или 'subscriptions'
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)

  // Загрузка постов
  useEffect(() => {
    const loadPosts = async () => {
      setLoading(true)
      try {
        // Fetch all posts from backend
        const allPosts = await postAPI.getAll()
        console.log("[Feed] Loaded posts from API:", allPosts)
        console.log("[Feed] Posts type:", typeof allPosts, "isArray:", Array.isArray(allPosts), "length:", Array.isArray(allPosts) ? allPosts.length : 'N/A')
        
        // Ensure we have an array
        const postsArray = Array.isArray(allPosts) ? allPosts : (allPosts ? [allPosts] : [])
        console.log("[Feed] Posts array after normalization:", postsArray.length)
        
        if (activeTab === "main") {
          // Show all posts, sorted by creation date (newest first)
          const sortedPosts = postsArray.length > 0
            ? postsArray.sort((a, b) => {
                const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
                const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
                return dateB - dateA
              })
            : []
          console.log("[Feed] Sorted posts for main tab:", sortedPosts.length)
          if (sortedPosts.length > 0) {
            console.log("[Feed] First post sample:", sortedPosts[0])
          }
          setPosts(sortedPosts)
        } else {
          // Show posts from subscribed users only
          const subscribedIds = subscribedUsers.map(u => u.id || u)
          if (subscribedIds.length === 0) {
            setPosts([])
          } else {
            const subscriptionPosts = postsArray.length > 0
              ? postsArray.filter(post => {
                  const authorId = post.author?.id || post.authorId
                  const matches = subscribedIds.includes(authorId)
                  return matches
                }).sort((a, b) => {
                  const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
                  const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
                  return dateB - dateA
                })
              : []
            console.log("[Feed] Filtered posts for subscriptions:", subscriptionPosts.length)
            setPosts(subscriptionPosts)
          }
        }
      } catch (error) {
        console.error("Ошибка загрузки постов:", error)
        // Fallback на моковые данные при ошибке
        try {
          const allPosts = activeTab === "main" ? getAllPosts() : getPostsByUserIds(subscribedUsers.map(u => u.id || u))
          console.log("[Feed] Using fallback mock data:", allPosts.length)
          setPosts(Array.isArray(allPosts) ? allPosts : [])
        } catch (fallbackError) {
          console.error("Ошибка загрузки моковых данных:", fallbackError)
          setPosts([])
        }
      } finally {
        setLoading(false)
      }
    }

    loadPosts()
  }, [activeTab, subscribedUsers])

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
            <FeedPostCard
              key={post.id}
              post={post}
              onNavigate={onNavigate}
              subscribedUsers={subscribedUsers}
              handleSubscribe={handleSubscribe}
            />
          ))
        )}
      </div>

      <BottomNavigation currentPage="home" onNavigate={onNavigate} />
    </div>
  )
}

export default Feed
