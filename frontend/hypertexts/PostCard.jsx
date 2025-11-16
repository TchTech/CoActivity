"use client"

import { useState } from "react"
import "../styles/post-card.css"

/**
 * Компонент карточки поста/комнаты
 */
function PostCard({ post }) {
  const [likes, setLikes] = useState(post.likes || 18)
  const [isLiked, setIsLiked] = useState(false)
  const [isSubscribed, setIsSubscribed] = useState(false)

  const handleLike = () => {
    if (isLiked) {
      setLikes(likes - 1)
    } else {
      setLikes(likes + 1)
    }
    setIsLiked(!isLiked)
  }

  const handleSubscribe = () => {
    setIsSubscribed(!isSubscribed)
  }

  return (
    <article className="post-card">
      <div className="post-header">
        <div className="post-author-info">
          <div className="post-author-avatar">👤</div>
          <span className="post-author-name">{post.authorName}</span>
        </div>

        <button className="post-subscribe-btn" onClick={handleSubscribe}>
          {isSubscribed ? "отписаться" : "подписаться"}
        </button>
      </div>

      <h2 className="post-title">{post.title}</h2>

      <div className="post-description">{post.description}</div>

      {post.imageUrl && <img src={post.imageUrl || "/placeholder.svg"} alt={post.title} className="post-image" />}

      <div className="post-actions">
        <div className="post-interactions">
          <button className="interaction-btn" onClick={handleLike} aria-label="Лайк">
            <span className="interaction-icon">{isLiked ? "❤️" : "🤍"}</span>
            {likes}
          </button>

          <button className="interaction-btn" aria-label="Комментарии">
            <span className="interaction-icon">💬</span>
            {post.comments || 4}
          </button>

          <button className="interaction-btn" aria-label="Поделиться">
            <span className="interaction-icon">↗️</span>
          </button>
        </div>

        <button className="join-button">Откликнуться</button>
      </div>
    </article>
  )
}

export default PostCard
