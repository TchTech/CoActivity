"use client"
import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { getUserById } from "../scripts/usersData"
import { getPostsByUserId } from "../scripts/postsData"
import { userAPI, postAPI, profileAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { usePostInteractions } from "../hooks/usePostInteractions"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/profile.css"

function ProfilePostCard({ post, userData, userRating, onNavigate }) {
  // Hook used at top of component, not inside a loop in another component.
  const postInteractions = usePostInteractions(post)

  return (
    <div
      key={post.id}
      className="post-card"
      onClick={() => {
        const postId = typeof post.id === "object" ? (post.id?.id || post.id?.postId || null) : post.id
        if (postId) {
          onNavigate("comments", postId)
        } else {
          console.error("[Profile] Invalid post.id:", post.id)
        }
      }}
    >
      <div className="post-header">
        <img src={userData.avatar || "/placeholder.svg"} alt={userData.name} className="avatar avatar-md" />
        <div className="post-user-info">
          <div className="post-username">
            {userData.name}
            {userRating !== null && (
              <span className="badge badge-rating">{userRating.toFixed(1)}</span>
            )}
          </div>
          <div className="post-time">
            {post.createdAt
              ? new Date(post.createdAt).toLocaleDateString("ru-RU")
              : post.time}
          </div>
        </div>
      </div>

      <h3 className="post-title">{post.name || post.title}</h3>
      <p className="post-content">{post.text || post.content}</p>

      {post.image && (
        <img
          src={
            typeof post.image === "object" && post.image.id
              ? `${imageAPI.getImageUrl ? imageAPI.getImageUrl(post.image.id) : `http://localhost:8080/images/${post.image.id}`}`
              : typeof post.image === "object"
              ? post.image.url || "/placeholder.svg"
              : post.image || "/placeholder.svg"
          }
          alt={post.name || post.title}
          className="post-image"
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
          onClick={(e) => {
            e.stopPropagation()
            if (post.id) {
              onNavigate("comments", post.id)
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
      </div>
    </div>
  )
}

function Profile({ onNavigate, userId }) {
  const { currentUser, subscribedUsers, subscribeToUser, unsubscribeFromUser } = useUser()
  const [userData, setUserData] = useState(null)
  const [userPosts, setUserPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [userRating, setUserRating] = useState(null)
  const [subscriptions, setSubscriptions] = useState([])
  const [followers, setFollowers] = useState([])
  const [avatarUploading, setAvatarUploading] = useState(false)

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

  // Определяем, какой профиль показывать
  // Wait for currentUser to load before defaulting
  // Only use currentUser.id if it's actually available and we're viewing own profile
  // Explicitly check for null/undefined to avoid falsy value issues (e.g., userId = 0)
  const profileUserId = (userId !== null && userId !== undefined && userId !== "") 
    ? userId 
    : ((currentUser?.id !== null && currentUser?.id !== undefined) ? currentUser.id : null)
  const isOwnProfile = (userId === null || userId === undefined || userId === "") 
    ? true 
    : (currentUser && userId === currentUser.id)

  // Загрузка данных профиля
  useEffect(() => {
    const loadProfile = async () => {
      // Don't load if we don't have a user ID yet
      if (!profileUserId) {
        setLoading(false)
        return
      }

      setLoading(true)
      try {
        // Загрузка профиля пользователя
        const profile = await userAPI.getProfile(profileUserId)
        if (profile) {
          setUserData(profile)
        } else {
          // Only use default if we're viewing own profile and it's not loaded yet
          if (isOwnProfile) {
            setUserData(defaultUser)
          } else {
            setUserData(null)
          }
        }

        // Загрузка рейтинга пользователя
        try {
          const ratingData = await userAPI.getRating(profileUserId)
          setUserRating(ratingData.rating || null)
        } catch (error) {
          console.error("Ошибка загрузки рейтинга:", error)
        }

        // Загрузка постов пользователя
        try {
          const posts = await postAPI.getByUser(profileUserId)
          // Sort by creation date (newest first)
          const sortedPosts = Array.isArray(posts)
            ? posts.sort((a, b) => {
                const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
                const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
                return dateB - dateA
              })
            : []
          setUserPosts(sortedPosts)
        } catch (error) {
          console.error("Ошибка загрузки постов пользователя:", error)
          // Fallback на моковые данные
          const posts = getPostsByUserId(profileUserId)
          setUserPosts(posts)
        }

        // Загрузка подписок и подписчиков
        if (profile) {
          try {
            const subs = profile.subscriptions || []
            const fols = profile.followers || []
            setSubscriptions(Array.isArray(subs) ? subs : [])
            setFollowers(Array.isArray(fols) ? fols : [])
          } catch (error) {
            console.error("Ошибка загрузки подписок/подписчиков:", error)
            setSubscriptions([])
            setFollowers([])
          }
        } else {
          setSubscriptions([])
          setFollowers([])
        }
      } catch (error) {
        console.error("Ошибка загрузки профиля:", error)
        // Fallback на моковые данные только для собственного профиля
        if (isOwnProfile) {
          const mockUser = getUserById(profileUserId) || defaultUser
          setUserData(mockUser)
          setUserPosts(getPostsByUserId(mockUser.id))
        } else {
          setUserData(null)
          setUserPosts([])
        }
        setSubscriptions([])
        setFollowers([])
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [profileUserId, userId, isOwnProfile, currentUser])

  const isSubscribed = subscribedUsers.some((u) => (u.id || u) === (userData?.id || profileUserId))


  const handleSubscription = async () => {
    if (!currentUser || !userData) return

    try {
      if (isSubscribed) {
        await unsubscribeFromUser(currentUser.id, userData.id)
      } else {
        await subscribeToUser(currentUser.id, userData.id)
      }
      // Reload profile to update subscriptions/followers
      const updatedProfile = await userAPI.getProfile(profileUserId)
      setUserData(updatedProfile || userData)
      const subs = updatedProfile?.subscriptions || []
      const fols = updatedProfile?.followers || []
      setSubscriptions(Array.isArray(subs) ? subs : [])
      setFollowers(Array.isArray(fols) ? fols : [])
    } catch (error) {
      console.error("Ошибка при подписке/отписке:", error)
    }
  }

  const handleAvatarUpload = async (e) => {
    if (!isOwnProfile || !currentUser) return
    
    const file = e.target.files[0]
    if (!file) return

    if (file.size > 10 * 1024 * 1024) {
      alert("Размер файла не должен превышать 10 МБ")
      return
    }

    setAvatarUploading(true)
    try {
      const updatedUser = await profileAPI.uploadAvatar(currentUser.id, file)
      setUserData(updatedUser || userData)
      // Update current user in context
      if (updatedUser) {
        currentUser.avatar = updatedUser.avatar
      }
    } catch (error) {
      console.error("Ошибка загрузки аватара:", error)
      alert("Ошибка при загрузке аватара. Попробуйте еще раз.")
    } finally {
      setAvatarUploading(false)
    }
  }


  if (loading || !userData) {
    return (
      <div>
        <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
          Загрузка профиля...
        </div>
      </div>
    )
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
            {isOwnProfile ? (
              <label style={{ position: "relative", cursor: "pointer" }}>
                <img
                  src={
                    userData.avatar?.id
                      ? `${imageAPI.getImageUrl ? imageAPI.getImageUrl(userData.avatar.id) : `http://localhost:8080/images/${userData.avatar.id}`}`
                      : userData.avatar || "/placeholder.svg"
                  }
                  alt={userData.name}
                  className="avatar avatar-xl"
                  style={{ opacity: avatarUploading ? 0.5 : 1 }}
                />
                {avatarUploading && (
                  <div style={{
                    position: "absolute",
                    top: "50%",
                    left: "50%",
                    transform: "translate(-50%, -50%)",
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-primary)",
                  }}>
                    Загрузка...
                  </div>
                )}
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleAvatarUpload}
                  style={{ display: "none" }}
                  disabled={avatarUploading}
                />
              </label>
            ) : (
              <img
                src={
                  userData.avatar?.id
                    ? `${imageAPI.getImageUrl ? imageAPI.getImageUrl(userData.avatar.id) : `http://localhost:8080/images/${userData.avatar.id}`}`
                    : userData.avatar || "/placeholder.svg"
                }
                alt={userData.name}
                className="avatar avatar-xl"
              />
            )}
            {userRating !== null && (
              <div className="profile-rating">{userRating.toFixed(1)}</div>
            )}
          </div>

          <h1 className="profile-name">{userData.name}</h1>
          <p className="profile-location">{userData.city}</p>

          {!isOwnProfile && currentUser && (
            <div className="profile-actions">
              <button className={`btn ${isSubscribed ? "btn-secondary" : "btn-primary"}`} onClick={handleSubscription}>
                {isSubscribed ? "Отписаться" : "Подписаться"}
              </button>
              <button className="btn btn-secondary">Сообщение</button>
            </div>
          )}

          <div className="profile-stats">
            <div className="stat-item">
              <div className="stat-value">{followers.length || userData.followers?.length || userData.followers || 0}</div>
              <div className="stat-label">Подписчики</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">{userData.rooms?.length || userData.rooms || 0}</div>
              <div className="stat-label">Комнаты</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">{subscriptions.length || userData.subscriptions?.length || userData.subscriptions || 0}</div>
              <div className="stat-label">Подписки</div>
            </div>
          </div>
        </div>

        <div className="profile-about">
          <h2>О человеке</h2>
          <p className="profile-about-text">{userData.about}</p>

          <div className="profile-tags">
            {(userData.interests || []).map((interest, index) => (
              <span key={index} className="tag">
                {typeof interest === "object" ? interest.name || interest : interest}
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
              <ProfilePostCard
                key={post.id}
                post={post}
                userData={userData}
                userRating={userRating}
                onNavigate={onNavigate}
              />
            ))
          )}
        </div>
      </div>

      {isOwnProfile && <BottomNavigation currentPage="profile" onNavigate={onNavigate} />}
    </div>
  )
}

export default Profile
