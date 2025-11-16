"use client"
import { useState } from "react"
import BottomNavigation from "./BottomNavigation"
import { getUserById } from "../scripts/usersData"
import { getPostsByUserId } from "../scripts/postsData"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/profile.css"

function Profile({ onNavigate, userId }) {
  const [subscribedUsers, setSubscribedUsers] = useState([1, 4, 6, 8])

  const defaultUser = {
    id: 1,
    name: "Петр Сидоров",
    city: "г. Саратов",
    rating: 8.5,
    followers: 106,
    rooms: 12,
    subscriptions: 80,
    about:
      "Занимаюсь на кружках по программированию, в детстве играл в настольный теннис день за днем, конструировал роботов на робототехнике в 5 классе - я гений. Собрал робот-пылесос, когда старый сломался, в 16 лет выиграл олимпиаду Изумруд по информационной безопасности. Общительный, физик-механик чуть-чуть, на любительском уровне. Собираюсь с друзьями и читаю труды Альберта Эйнштейна по теории относительности.",
    interests: ["программирование", "робототехника", "физика-механика", "настольный теннис"],
    avatar: "/male-avatar.png",
    verified: true,
  }

  const userData = userId ? getUserById(userId) || defaultUser : defaultUser
  const isOwnProfile = !userId || userId === 1
  const isSubscribed = subscribedUsers.includes(userData.id)

  const userPosts = getPostsByUserId(userData.id)

  const handleSubscription = () => {
    if (isSubscribed) {
      setSubscribedUsers(subscribedUsers.filter((id) => id !== userData.id))
    } else {
      setSubscribedUsers([...subscribedUsers, userData.id])
    }
  }

  return (
    <div>
      <div className="profile-container">
        {!isOwnProfile && (
          <div className="top-nav">
            <button className="btn-icon" onClick={() => onNavigate("home")}>
              ←
            </button>
            <div className="top-nav-title">Профиль</div>
            <div style={{ width: "40px" }}></div>
          </div>
        )}

        <div className="profile-header">
          <div className="profile-avatar-section">
            <img src={userData.avatar || "/placeholder.svg"} alt={userData.name} className="avatar avatar-xl" />
            <div className="profile-rating">{userData.rating}</div>
          </div>

          <h1 className="profile-name">{userData.name}</h1>
          <p className="profile-location">{userData.city}</p>

          {!isOwnProfile && (
            <div className="profile-actions">
              <button className={`btn ${isSubscribed ? "btn-secondary" : "btn-primary"}`} onClick={handleSubscription}>
                {isSubscribed ? "Отписаться" : "Подписаться"}
              </button>
              <button className="btn btn-secondary">Сообщение</button>
            </div>
          )}

          <div className="profile-stats">
            <div className="stat-item">
              <div className="stat-value">{userData.followers}</div>
              <div className="stat-label">Подписчики</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">{userData.rooms}</div>
              <div className="stat-label">Комнаты</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">{userData.subscriptions}</div>
              <div className="stat-label">Подписки</div>
            </div>
          </div>
        </div>

        <div className="profile-about">
          <h2>О человеке</h2>
          <p className="profile-about-text">{userData.about}</p>

          <div className="profile-tags">
            {userData.interests.map((interest, index) => (
              <span key={index} className="tag">
                {interest}
              </span>
            ))}
          </div>
        </div>

        <div className="profile-content-section">
          <h2 className="profile-content-header">Контент</h2>

          {userPosts.length === 0 ? (
            <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
              Нет постов
            </div>
          ) : (
            userPosts.map((post) => (
              <div key={post.id} className="post-card" onClick={() => onNavigate("comments", post.id)}>
                <div className="post-header">
                  <img src={userData.avatar || "/placeholder.svg"} alt={userData.name} className="avatar avatar-md" />
                  <div className="post-user-info">
                    <div className="post-username">
                      {userData.name}
                      <span className="badge badge-rating">{userData.rating}</span>
                    </div>
                    <div className="post-time">{post.time}</div>
                  </div>
                </div>

                <h3 className="post-title">{post.title}</h3>
                <p className="post-content">{post.content}</p>

                {post.image && <img src={post.image || "/placeholder.svg"} alt={post.title} className="post-image" />}

                <div className="post-actions">
                  <button className="post-action-btn">
                    <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </svg>
                    <span>{post.likes}</span>
                  </button>
                  <button className="post-action-btn">
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
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {isOwnProfile && <BottomNavigation currentPage="profile" onNavigate={onNavigate} />}
    </div>
  )
}

export default Profile
