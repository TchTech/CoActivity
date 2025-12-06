"use client"
import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
// Mock data imports removed - using API only
import { userAPI, postAPI, profileAPI, imageAPI, externalLinksAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { usePostInteractions } from "../hooks/usePostInteractions"
import { ConfirmDeleteDialog } from "../components/ConfirmDeleteDialog"
import { MyApplicationsList } from "../components/rooms"
import PostRoomJoinButton from "../components/PostRoomJoinButton"
import { getAvatarEmoji } from "../utils/avatarUtils"
import { AlertDialog } from "../components/ui/AlertDialog"
import { ConfirmDialog } from "../components/ui/ConfirmDialog"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/profile.css"

// Function to get rating color based on score (0-10 scale)
function getRatingColor(rating) {
  if (rating === null || rating === undefined || isNaN(rating)) {
    return "var(--text-muted)"
  }
  if (rating >= 8.5) return "#22c55e" // green
  if (rating >= 7.0) return "#84cc16" // lime
  if (rating >= 5.5) return "#eab308" // yellow
  if (rating >= 4.0) return "#f97316" // orange
  return "#ef4444" // red
}

function ProfilePostCard({ post, userData, ratingSummary, onNavigate, currentUser, onDeletePost }) {
  const [isDeleting, setIsDeleting] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  
  const isAuthor = currentUser && (post.author?.id === currentUser.id || post.userId === currentUser.id)
  
  const handleDeleteClick = (e) => {
    e.stopPropagation()
    if (!currentUser || !isAuthor) {
      return
    }
    setShowDeleteDialog(true)
  }
  
  const handleDeleteConfirm = async (e) => {
    // Предотвращаем всплытие события, чтобы не вызвать навигацию
    if (e) {
      e.stopPropagation()
      e.preventDefault()
    }
    
    setIsDeleting(true)
    setShowDeleteDialog(false) // Закрываем диалог сразу
    
    const postId = typeof post.id === "object" ? (post.id?.id || post.id?.postId || null) : post.id
    
    // Оптимистичное обновление: сразу удаляем пост из списка, чтобы он исчез из UI
    if (postId && onDeletePost) {
      onDeletePost(postId)
    }
    
    try {
      if (postId) {
        await postAPI.delete(postId, currentUser.id)
        // Пост уже удален из списка выше, здесь просто логируем успех
        console.log("Пост успешно удален:", postId)
      }
    } catch (error) {
      console.error("Ошибка при удалении поста:", error)
      // Если ошибка 404, значит пост уже удален - это нормально, пост уже скрыт
      if (error.status === 404 || (error.message && error.message.includes("404"))) {
        console.log("Пост уже был удален на сервере")
      } else {
        // При другой ошибке показываем сообщение, но пост уже скрыт из UI
        setAlertData({ title: "Предупреждение", message: "Не удалось удалить пост на сервере, но он уже скрыт из списка.", variant: "warning" })
        setShowAlert(true)
      }
    } finally {
      setIsDeleting(false)
    }
  }
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
        {userData.avatar?.id ? (
          <img 
            src={imageAPI.getImageUrl(userData.avatar.id)} 
            alt={userData.name} 
            className="avatar avatar-md" 
          />
        ) : (
          <div
            className="avatar avatar-md"
            style={{
              backgroundColor: "transparent",
              border: "none",
              width: "40px",
              height: "40px"
            }}
          />
        )}
        <div className="post-user-info">
          <div className="post-username">
            {userData.name}
            {ratingSummary?.average != null && typeof ratingSummary.average === 'number' && !isNaN(ratingSummary.average) && (
              <span 
                className="badge badge-rating"
                style={{
                  backgroundColor: getRatingColor(ratingSummary.average),
                  color: "white"
                }}
              >
                {ratingSummary.average.toFixed(1)}
              </span>
            )}
          </div>
          <div className="post-time">
            {post.createdAt
              ? new Date(post.createdAt).toLocaleDateString("ru-RU")
              : post.time}
          </div>
        </div>
        {isAuthor && (
          <>
            <button
              className="btn btn-secondary"
              onClick={handleDeleteClick}
              disabled={isDeleting}
              style={{ fontSize: "var(--font-size-sm)", padding: "6px 12px" }}
              title="Удалить пост"
            >
              {isDeleting ? "..." : "🗑️"}
            </button>
            <ConfirmDeleteDialog
              open={showDeleteDialog}
              onOpenChange={(open) => {
                // Предотвращаем закрытие диалога через клик вне его, если идет удаление
                if (!isDeleting) {
                  setShowDeleteDialog(open)
                }
              }}
              onConfirm={handleDeleteConfirm}
            />
          </>
        )}
      </div>

      <h3 className="post-title">{post.name || post.title}</h3>
      <p className="post-content">{post.text || post.content}</p>

      {/* Show room label if post is attached to a room */}
      {post.room && (
        <PostRoomJoinButton post={post} onNavigate={onNavigate} />
      )}

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
          <img 
            src="/like.png" 
            alt="Лайк" 
            className="post-action-icon"
            style={{ width: '20px', height: '20px', objectFit: 'contain' }}
          />
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
          <img 
            src="/dislike.png" 
            alt="Дизлайк" 
            className="post-action-icon"
            style={{ width: '20px', height: '20px', objectFit: 'contain' }}
          />
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

function Profile({ onNavigate, userId, currentPage }) {
  const { currentUser, subscribedUsers, subscribeToUser, unsubscribeFromUser } = useUser()
  const [userData, setUserData] = useState(null)
  const [userPosts, setUserPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [userRating, setUserRating] = useState(null)
  const [subscriptions, setSubscriptions] = useState([])
  const [followers, setFollowers] = useState([])
  const [avatarUploading, setAvatarUploading] = useState(false)
  const [roomCount, setRoomCount] = useState(0)
  const [externalLinks, setExternalLinks] = useState([])
  const [activeTab, setActiveTab] = useState("posts") // "posts", "about", "applications"
  const [ratingSummary, setRatingSummary] = useState({ average: null, count: 0 })
  const [isEditingAbout, setIsEditingAbout] = useState(false)
  const [aboutText, setAboutText] = useState("")
  const [showAlert, setShowAlert] = useState(false)
  const [alertData, setAlertData] = useState({ title: "", message: "", variant: "info" })

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

        // Рейтинг загружается через getRatingSummary ниже

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
          setUserPosts([])
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

        // Загрузка количества комнат
        try {
          const countData = await userAPI.getRoomCount(profileUserId)
          setRoomCount(countData.count || 0)
        } catch (error) {
          console.error("Ошибка загрузки количества комнат:", error)
          setRoomCount(0)
        }

        // Загрузка внешних ссылок
        try {
          const links = await externalLinksAPI.get(profileUserId)
          setExternalLinks(Array.isArray(links) ? links : [])
        } catch (error) {
          console.error("Ошибка загрузки внешних ссылок:", error)
          setExternalLinks([])
        }

        // Загрузка рейтинга
        try {
          const summary = await userAPI.getRatingSummary(profileUserId)
          // Ensure summary has valid structure
          if (summary && typeof summary === 'object') {
            setRatingSummary({
              average: summary.average != null && !isNaN(summary.average) ? Number(summary.average) : null,
              count: summary.count != null ? Number(summary.count) : 0
            })
          } else {
            setRatingSummary({ average: null, count: 0 })
          }
        } catch (error) {
          console.error("Ошибка загрузки рейтинга:", error)
          setRatingSummary({ average: null, count: 0 })
        }

        // Загрузка about
        try {
          const aboutData = await userAPI.getAbout(profileUserId)
          setAboutText(aboutData.about || "")
        } catch (error) {
          console.error("Ошибка загрузки about:", error)
        }
      } catch (error) {
        console.error("Ошибка загрузки профиля:", error)
        setUserData(null)
        setUserPosts([])
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
        // Unsubscribe
        try {
          await unsubscribeFromUser(currentUser.id, userData.id)
        } catch (error) {
          // If already unsubscribed, that's fine - just update UI
          if (error.message && error.message.includes("409")) {
            console.log("User already unsubscribed, updating UI")
          } else {
            throw error
          }
        }
      } else {
        // Subscribe - check if already subscribed first
        const alreadySubscribed = subscribedUsers.some(
          (u) => (u.id || u) === userData.id
        ) || subscriptions.some(
          (sub) => (sub.id || sub) === userData.id
        )
        
        if (alreadySubscribed) {
          console.log("User already subscribed, skipping API call")
          // Update UI to reflect subscription state
          setSubscriptions([...subscriptions, userData])
          return
        }

        try {
          await subscribeToUser(currentUser.id, userData.id)
        } catch (error) {
          // Handle 409 conflict gracefully
          if (error.message && (error.message.includes("409") || error.message.includes("already"))) {
            console.log("User already subscribed, updating UI")
            // Update UI to reflect subscription state
            setSubscriptions([...subscriptions, userData])
            return
          } else {
            throw error
          }
        }
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
      // Show user-friendly error message
      const errorMsg = error.message || "Неизвестная ошибка"
      if (errorMsg.includes("409") || errorMsg.includes("already")) {
        // Already handled above, but just in case
        console.log("Subscription conflict handled")
      } else {
        setAlertData({ title: "Ошибка", message: "Не удалось выполнить действие: " + errorMsg, variant: "error" })
        setShowAlert(true)
      }
    }
  }

  const handleAvatarUpload = async (e) => {
    if (!isOwnProfile || !currentUser) return
    
    const file = e.target.files[0]
    if (!file) return

    if (file.size > 10 * 1024 * 1024) {
      setAlertData({ title: "Ошибка", message: "Размер файла не должен превышать 10 МБ", variant: "error" })
      setShowAlert(true)
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
      setAlertData({ title: "Ошибка", message: "Ошибка при загрузке аватара. Попробуйте еще раз.", variant: "error" })
      setShowAlert(true)
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
        {isOwnProfile && (
          <div className="top-nav">
            <div style={{ width: "40px" }}></div>
            <div className="top-nav-title">Профиль</div>
            <div className="top-nav-actions">
              <button 
                className="btn-icon" 
                onClick={() => onNavigate("settings")}
                aria-label="Настройки"
                style={{
                  width: "40px",
                  height: "40px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center"
                }}
              >
                <svg 
                  viewBox="0 0 24 24" 
                  fill="none" 
                  stroke="currentColor" 
                  strokeWidth="2"
                  style={{ 
                    width: "24px", 
                    height: "24px"
                  }}
                >
                  <circle cx="12" cy="12" r="3" />
                  <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m4.24 4.24l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m4.24-4.24l4.24-4.24" />
                </svg>
              </button>
            </div>
          </div>
        )}

        <div className="profile-header">
          <div className="profile-avatar-section">
            {isOwnProfile ? (
              <div style={{ position: "relative", display: "inline-block" }}>
                <label style={{ position: "relative", cursor: "pointer", display: "block" }}>
                  {userData.avatar?.id ? (
                    <img
                      src={imageAPI.getImageUrl(userData.avatar.id)}
                      alt={userData.name}
                      className="avatar avatar-xl"
                      style={{ opacity: avatarUploading ? 0.5 : 1 }}
                    />
                  ) : (
                    <div
                      className="avatar avatar-xl"
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        backgroundColor: "var(--bg-tertiary)",
                        border: "1px solid var(--border-primary)",
                        borderRadius: "50%",
                        fontSize: "var(--font-size-xl)",
                        opacity: avatarUploading ? 0.5 : 1
                      }}
                    >
                      {getAvatarEmoji(userData.id)}
                    </div>
                  )}
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
                    id="avatar-upload-input"
                  />
                </label>
                {/* Кнопка-иконка для загрузки аватарки */}
                <label
                  htmlFor="avatar-upload-input"
                  className="avatar-upload-button"
                  style={{
                    pointerEvents: avatarUploading ? "none" : "auto",
                    opacity: avatarUploading ? 0.6 : 1,
                  }}
                  title="Изменить аватар"
                >
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="var(--bg-primary)"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                    <circle cx="12" cy="13" r="4" />
                  </svg>
                </label>
              </div>
            ) : (
              userData.avatar?.id ? (
                <img
                  src={imageAPI.getImageUrl(userData.avatar.id)}
                  alt={userData.name}
                  className="avatar avatar-xl"
                />
              ) : (
                <div
                  className="avatar avatar-xl"
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    backgroundColor: "var(--bg-tertiary)",
                    border: "1px solid var(--border-primary)",
                    borderRadius: "50%",
                    fontSize: "var(--font-size-xl)",
                  }}
                >
                  {getAvatarEmoji(userData.id)}
                </div>
              )
            )}
            {ratingSummary.average != null && typeof ratingSummary.average === 'number' && !isNaN(ratingSummary.average) && (
              <div 
                className="profile-rating"
                style={{
                  position: "absolute",
                  bottom: "-10px",
                  left: "-10px",
                  backgroundColor: getRatingColor(ratingSummary.average),
                  color: "white",
                  borderRadius: "50%",
                  width: "40px",
                  height: "40px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "var(--font-size-sm)",
                  fontWeight: "600",
                  border: "2px solid var(--bg-primary)",
                }}
                title={`Средний рейтинг: ${ratingSummary.average.toFixed(1)} (${ratingSummary.count || 0} оценок)`}
              >
                {ratingSummary.average.toFixed(1)}
              </div>
            )}
          </div>

          <h1 className="profile-name">{userData.name}</h1>
          <p className="profile-location">{userData.city}</p>

          {!isOwnProfile && currentUser && (
            <div className="profile-actions">
              <button className={`btn ${isSubscribed ? "btn-secondary" : "btn-primary"}`} onClick={handleSubscription}>
                {isSubscribed ? "Отписаться" : "Подписаться"}
              </button>
            </div>
          )}

          <div className="profile-stats">
            <div className="stat-item">
              <div className="stat-value">{followers.length || userData.followers?.length || userData.followers || 0}</div>
              <div className="stat-label">Подписчики</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">{roomCount}</div>
              <div className="stat-label">Комнаты</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">{subscriptions.length || userData.subscriptions?.length || userData.subscriptions || 0}</div>
              <div className="stat-label">Подписки</div>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="tabs">
          <button 
            className={`tab ${activeTab === "posts" ? "active" : ""}`} 
            onClick={() => setActiveTab("posts")}
          >
            Посты
          </button>
          <button 
            className={`tab ${activeTab === "about" ? "active" : ""}`} 
            onClick={() => setActiveTab("about")}
          >
            О себе
          </button>
          {isOwnProfile && (
            <button
              className={`tab ${activeTab === "applications" ? "active" : ""}`} 
              onClick={() => setActiveTab("applications")}
            >
              Мои заявки
            </button>
          )}
        </div>

        {activeTab === "posts" ? (
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
                  ratingSummary={ratingSummary}
                  onNavigate={onNavigate}
                  currentUser={currentUser}
                  onDeletePost={(postId) => {
                    // Удаляем пост из списка постов
                    setUserPosts((prevPosts) => prevPosts.filter((p) => {
                      const pId = typeof p.id === "object" ? (p.id?.id || p.id?.postId || null) : p.id
                      return pId !== postId
                    }))
                  }}
                />
              ))
            )}
          </div>
        ) : activeTab === "about" ? (
          <div className="profile-about">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "var(--spacing-md)" }}>
              <h2>О человеке</h2>
              {isOwnProfile && (
                <button
                  className="btn btn-sm"
                  onClick={() => {
                    if (isEditingAbout) {
                      // Save
                      userAPI.updateAbout(profileUserId, currentUser.id, aboutText)
                        .then(() => {
                          setIsEditingAbout(false)
                          setUserData({ ...userData, about: aboutText })
                          setAlertData({ title: "Успешно", message: "Описание обновлено", variant: "success" })
                          setShowAlert(true)
                        })
                        .catch(err => {
                          console.error("Ошибка сохранения about:", err)
                          setAlertData({ title: "Ошибка", message: "Не удалось сохранить изменения", variant: "error" })
                          setShowAlert(true)
                        })
                    } else {
                      // Edit
                      setIsEditingAbout(true)
                    }
                  }}
                  style={{
                    padding: "8px 16px",
                    fontSize: "var(--font-size-sm)",
                    border: "2px solid var(--accent-gold)",
                    borderRadius: "8px",
                    backgroundColor: "transparent",
                    color: "var(--accent-gold)",
                    fontWeight: "600",
                    cursor: "pointer",
                    transition: "all 0.2s ease",
                  }}
                >
                  {isEditingAbout ? "✓ Сохранить" : "✏️ Редактировать"}
                </button>
              )}
            </div>
            {isEditingAbout ? (
              <textarea
                value={aboutText}
                onChange={(e) => setAboutText(e.target.value)}
                placeholder="Расскажите о себе..."
                maxLength={500}
                style={{
                  width: "100%",
                  minHeight: "120px",
                  padding: "12px 16px",
                  borderRadius: "8px",
                  border: "2px solid var(--accent-gold)",
                  fontSize: "var(--font-size-base)",
                  fontFamily: "inherit",
                  resize: "vertical",
                  backgroundColor: "var(--bg-primary)",
                  color: "var(--text-primary)",
                  transition: "all 0.2s ease",
                  outline: "none",
                  boxSizing: "border-box",
                }}
                onFocus={(e) => {
                  e.target.style.boxShadow = "0 0 0 3px rgba(255, 215, 0, 0.1)"
                }}
                onBlur={(e) => {
                  e.target.style.boxShadow = "none"
                }}
              />
            ) : (
              <p className="profile-about-text" style={{
                padding: "var(--spacing-md)",
                backgroundColor: "var(--bg-secondary)",
                borderRadius: "8px",
                border: "1px solid var(--border-primary)",
                minHeight: "60px",
                lineHeight: "1.6",
              }}>
                {aboutText || userData.about || "информация не указана"}
              </p>
            )}

            {/* Интересы с возможностью редактирования */}
            <div style={{ marginTop: "var(--spacing-lg)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "var(--spacing-md)" }}>
                <h3 style={{ fontSize: "var(--font-size-lg)", margin: 0 }}>Интересы</h3>
                {isOwnProfile && (
                  <button
                    onClick={() => onNavigate("settings")}
                    style={{
                      padding: "6px 12px",
                      fontSize: "var(--font-size-sm)",
                      border: "2px solid var(--accent-gold)",
                      borderRadius: "6px",
                      backgroundColor: "transparent",
                      color: "var(--accent-gold)",
                      fontWeight: "600",
                      cursor: "pointer",
                      transition: "all 0.2s ease",
                    }}
                  >
                    ✏️ Изменить
                  </button>
                )}
              </div>
              <div className="profile-tags" style={{ 
                display: "flex", 
                flexWrap: "wrap", 
                gap: "var(--spacing-sm)",
                padding: userData.interests?.length > 0 ? "var(--spacing-md)" : "var(--spacing-sm)",
                backgroundColor: "var(--bg-secondary)",
                borderRadius: "8px",
                border: "1px solid var(--border-primary)",
                minHeight: "50px",
              }}>
                {(userData.interests && userData.interests.length > 0) ? (
                  userData.interests.map((interest, index) => (
                    <span 
                      key={index} 
                      className="tag"
                      style={{
                        padding: "8px 16px",
                        backgroundColor: "var(--accent-gold)",
                        color: "var(--bg-primary)",
                        borderRadius: "20px",
                        fontSize: "var(--font-size-sm)",
                        fontWeight: "600",
                      }}
                    >
                      {typeof interest === "object" ? interest.name || interest : interest}
                    </span>
                  ))
                ) : (
                  <span style={{ color: "var(--text-muted)", fontSize: "var(--font-size-sm)" }}>
                    Интересы не указаны
                  </span>
                )}
              </div>
            </div>

            {/* External Links */}
            <div style={{ marginTop: "var(--spacing-lg)" }}>
              <h3 style={{ fontSize: "var(--font-size-lg)", marginBottom: "var(--spacing-md)" }}>Внешние ссылки</h3>
              {isOwnProfile ? (
                <ExternalLinksEditor 
                  userId={profileUserId} 
                  links={externalLinks} 
                  onLinksChange={setExternalLinks}
                />
              ) : (
                <ExternalLinksList links={externalLinks} />
              )}
            </div>
          </div>
        ) : activeTab === "applications" && isOwnProfile ? (
          <div style={{ padding: "var(--spacing-md)", paddingBottom: "80px" }}>
            <MyApplicationsList
              userId={profileUserId}
              onRequestCancelled={() => {
                // Component handles its own refresh
              }}
              onError={(error) => {
                setAlertData({ title: "Ошибка", message: error, variant: "error" })
                setShowAlert(true)
              }}
              onNavigate={(path) => {
                // Handle navigation - path format: "rooms/{roomId}"
                const parts = path.split("/")
                if (parts[0] === "rooms" && parts[1]) {
                  onNavigate("roomInfo", parseInt(parts[1]))
                }
              }}
            />
          </div>
        ) : null}
      </div>

      {isOwnProfile && <BottomNavigation currentPage={currentPage || "profile"} onNavigate={onNavigate} />}

      {/* Alert Dialog */}
      <AlertDialog
        open={showAlert}
        title={alertData.title}
        message={alertData.message}
        variant={alertData.variant}
        onClose={() => setShowAlert(false)}
      />
    </div>
  )
}

function ExternalLinksList({ links }) {
  if (!links || links.length === 0) {
    return <div style={{ color: "var(--text-muted)", fontSize: "var(--font-size-sm)" }}>Нет внешних ссылок</div>
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-sm)" }}>
      {links.map((link) => (
        <a
          key={link.id}
          href={link.url}
          target="_blank"
          rel="noopener noreferrer"
          style={{
            display: "flex",
            alignItems: "center",
            gap: "var(--spacing-sm)",
            padding: "var(--spacing-sm)",
            backgroundColor: "var(--bg-secondary)",
            borderRadius: "var(--radius-md)",
            textDecoration: "none",
            color: "var(--accent-blue)",
          }}
        >
          <span style={{ fontWeight: "600" }}>{link.label || link.platformName || "Ссылка"}</span>
          <span style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)" }}>{link.url}</span>
        </a>
      ))}
    </div>
  )
}

function ExternalLinksEditor({ userId, links, onLinksChange }) {
  const [editingLink, setEditingLink] = useState(null)
  const [newLink, setNewLink] = useState({ platformName: "", label: "", url: "" })
  const [saving, setSaving] = useState(false)
  const [showAlert, setShowAlert] = useState(false)
  const [alertData, setAlertData] = useState({ title: "", message: "", variant: "info" })
  const [showDeleteLinkConfirm, setShowDeleteLinkConfirm] = useState(false)
  const [linkToDelete, setLinkToDelete] = useState(null)

  const handleAddLink = async () => {
    if (!newLink.url || !newLink.url.trim()) {
      setAlertData({ title: "Ошибка", message: "URL обязателен", variant: "error" })
      setShowAlert(true)
      return
    }

    setSaving(true)
    try {
      const created = await externalLinksAPI.create(userId, newLink)
      onLinksChange([...links, created])
      setNewLink({ platformName: "", label: "", url: "" })
    } catch (error) {
      console.error("Ошибка добавления ссылки:", error)
      setAlertData({ title: "Ошибка", message: "Не удалось добавить ссылку: " + (error.message || "Неизвестная ошибка"), variant: "error" })
      setShowAlert(true)
    } finally {
      setSaving(false)
    }
  }

  const handleUpdateLink = async (linkId, updatedLink) => {
    setSaving(true)
    try {
      const updated = await externalLinksAPI.update(userId, linkId, updatedLink)
      onLinksChange(links.map(l => l.id === linkId ? updated : l))
      setEditingLink(null)
    } catch (error) {
      console.error("Ошибка обновления ссылки:", error)
      setAlertData({ title: "Ошибка", message: "Не удалось обновить ссылку: " + (error.message || "Неизвестная ошибка"), variant: "error" })
      setShowAlert(true)
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteLink = async (linkId) => {
    setLinkToDelete(linkId)
    setShowDeleteLinkConfirm(true)
  }

  const confirmDeleteLink = async () => {
    if (!linkToDelete) return
    
    setShowDeleteLinkConfirm(false)
    setSaving(true)
    try {
      await externalLinksAPI.delete(userId, linkToDelete)
      onLinksChange(links.filter(l => l.id !== linkToDelete))
      setLinkToDelete(null)
    } catch (error) {
      console.error("Ошибка удаления ссылки:", error)
      setAlertData({ title: "Ошибка", message: "Не удалось удалить ссылку: " + (error.message || "Неизвестная ошибка"), variant: "error" })
      setShowAlert(true)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-sm)", marginBottom: "var(--spacing-md)" }}>
        <input
          type="text"
          placeholder="Название платформы (например, GitHub)"
          value={newLink.platformName}
          onChange={(e) => setNewLink({ ...newLink, platformName: e.target.value })}
          style={{ padding: "var(--spacing-sm)", borderRadius: "var(--radius-md)", border: "1px solid var(--border-color)" }}
        />
        <input
          type="text"
          placeholder="Метка (необязательно)"
          value={newLink.label}
          onChange={(e) => setNewLink({ ...newLink, label: e.target.value })}
          style={{ padding: "var(--spacing-sm)", borderRadius: "var(--radius-md)", border: "1px solid var(--border-color)" }}
        />
        <input
          type="url"
          placeholder="URL (например, https://github.com/username)"
          value={newLink.url}
          onChange={(e) => setNewLink({ ...newLink, url: e.target.value })}
          style={{ padding: "var(--spacing-sm)", borderRadius: "var(--radius-md)", border: "1px solid var(--border-color)" }}
        />
        <button 
          className="btn btn-primary" 
          onClick={handleAddLink}
          disabled={saving || !newLink.url}
        >
          {saving ? "Сохранение..." : "Добавить ссылку"}
        </button>
      </div>

      {links.length > 0 && (
        <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-sm)" }}>
          {links.map((link) => (
            editingLink === link.id ? (
              <LinkEditForm
                key={link.id}
                link={link}
                onSave={(updated) => handleUpdateLink(link.id, updated)}
                onCancel={() => setEditingLink(null)}
              />
            ) : (
              <div
                key={link.id}
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "var(--spacing-sm)",
                  backgroundColor: "var(--bg-secondary)",
                  borderRadius: "var(--radius-md)",
                }}
              >
                <a
                  href={link.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ textDecoration: "none", color: "var(--accent-blue)", flex: 1 }}
                >
                  <div style={{ fontWeight: "600" }}>{link.label || link.platformName || "Ссылка"}</div>
                  <div style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)" }}>{link.url}</div>
                </a>
                <div style={{ display: "flex", gap: "var(--spacing-xs)" }}>
                  <button
                    className="btn btn-sm"
                    onClick={() => setEditingLink(link.id)}
                    style={{ padding: "4px 8px" }}
                  >
                    Изменить
                  </button>
                  <button
                    className="btn btn-sm"
                    onClick={() => handleDeleteLink(link.id)}
                    style={{ padding: "4px 8px", backgroundColor: "var(--error)" }}
                  >
                    Удалить
                  </button>
                </div>
              </div>
            )
          ))}
        </div>
      )}

      {/* Alert Dialog */}
      <AlertDialog
        open={showAlert}
        title={alertData.title}
        message={alertData.message}
        variant={alertData.variant}
        onClose={() => setShowAlert(false)}
      />

      {/* Confirm Dialog for deleting link */}
      <ConfirmDialog
        open={showDeleteLinkConfirm}
        title="Удалить ссылку"
        message="Удалить эту ссылку?"
        confirmText="Удалить"
        cancelText="Отмена"
        confirmVariant="destructive"
        onConfirm={confirmDeleteLink}
        onCancel={() => {
          setShowDeleteLinkConfirm(false)
          setLinkToDelete(null)
        }}
      />
    </div>
  )
}

function LinkEditForm({ link, onSave, onCancel }) {
  const [formData, setFormData] = useState({
    platformName: link.platformName || "",
    label: link.label || "",
    url: link.url || "",
  })

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-sm)", padding: "var(--spacing-sm)", backgroundColor: "var(--bg-secondary)", borderRadius: "var(--radius-md)" }}>
      <input
        type="text"
        placeholder="Название платформы"
        value={formData.platformName}
        onChange={(e) => setFormData({ ...formData, platformName: e.target.value })}
        style={{ padding: "var(--spacing-sm)", borderRadius: "var(--radius-md)", border: "1px solid var(--border-color)" }}
      />
      <input
        type="text"
        placeholder="Метка"
        value={formData.label}
        onChange={(e) => setFormData({ ...formData, label: e.target.value })}
        style={{ padding: "var(--spacing-sm)", borderRadius: "var(--radius-md)", border: "1px solid var(--border-color)" }}
      />
      <input
        type="url"
        placeholder="URL"
        value={formData.url}
        onChange={(e) => setFormData({ ...formData, url: e.target.value })}
        style={{ padding: "var(--spacing-sm)", borderRadius: "var(--radius-md)", border: "1px solid var(--border-color)" }}
      />
      <div style={{ display: "flex", gap: "var(--spacing-sm)" }}>
        <button className="btn btn-primary" onClick={() => onSave(formData)} style={{ flex: 1 }}>
          Сохранить
        </button>
        <button className="btn btn-secondary" onClick={onCancel} style={{ flex: 1 }}>
          Отмена
        </button>
      </div>
    </div>
  )
}

export default Profile
