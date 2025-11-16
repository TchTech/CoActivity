"use client"

import { useState } from "react"
import BottomNavigation from "./BottomNavigation"
import { getAllPosts, getPostsByUserIds } from "../scripts/postsData"
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
  const [activeTab, setActiveTab] = useState("main") // 'main' или 'subscriptions'
  const [subscribedUsers, setSubscribedUsers] = useState([1, 4, 6, 8]) // ID пользователей на которых подписан текущий юзер

  // Получаем посты в зависимости от активной вкладки
  const posts = activeTab === "main" ? getAllPosts() : getPostsByUserIds(subscribedUsers)

  const handlePostLike = (postId) => {
    console.log("[v0] Like post:", postId)
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
        {posts.length === 0 ? (
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
                  className={`btn ${subscribedUsers.includes(post.userId) ? "btn-secondary" : "btn-primary"}`}
                  onClick={(e) => {
                    e.stopPropagation()
                    if (subscribedUsers.includes(post.userId)) {
                      setSubscribedUsers(subscribedUsers.filter((id) => id !== post.userId))
                    } else {
                      setSubscribedUsers([...subscribedUsers, post.userId])
                    }
                  }}
                >
                  {subscribedUsers.includes(post.userId) ? "отписаться" : "подписаться"}
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
                <button className="post-action-btn" onClick={() => handlePostLike(post.id)}>
                  <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                  </svg>
                  <span>{post.likes}</span>
                </button>
                <button className="post-action-btn" onClick={() => onNavigate("comments", post.id)}>
                  <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                  <span>{post.comments}</span>
                </button>
                <button className="post-action-btn">
                  <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  <span>{post.views}</span>
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
