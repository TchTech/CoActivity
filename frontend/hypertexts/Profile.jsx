"use client"
import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { getUserById } from "../scripts/usersData"
import { getPostsByUserId } from "../scripts/postsData"
import { userAPI, postAPI, profileAPI, imageAPI, externalLinksAPI } from "../lib/api"
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
  const [roomCount, setRoomCount] = useState(0)
  const [externalLinks, setExternalLinks] = useState([])
  const [activeTab, setActiveTab] = useState("posts") // "posts", "about"

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
                  userRating={userRating}
                  onNavigate={onNavigate}
                />
              ))
            )}
          </div>
        ) : (
          <div className="profile-about">
            <h2>О человеке</h2>
            <p className="profile-about-text">{userData.about || "Информация не указана"}</p>

            <div className="profile-tags">
              {(userData.interests || []).map((interest, index) => (
                <span key={index} className="tag">
                  {typeof interest === "object" ? interest.name || interest : interest}
                </span>
              ))}
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
        )}
      </div>

      {isOwnProfile && <BottomNavigation currentPage="profile" onNavigate={onNavigate} />}
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

  const handleAddLink = async () => {
    if (!newLink.url || !newLink.url.trim()) {
      alert("URL обязателен")
      return
    }

    setSaving(true)
    try {
      const created = await externalLinksAPI.create(userId, newLink)
      onLinksChange([...links, created])
      setNewLink({ platformName: "", label: "", url: "" })
    } catch (error) {
      console.error("Ошибка добавления ссылки:", error)
      alert("Не удалось добавить ссылку: " + (error.message || "Неизвестная ошибка"))
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
      alert("Не удалось обновить ссылку: " + (error.message || "Неизвестная ошибка"))
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteLink = async (linkId) => {
    if (!confirm("Удалить эту ссылку?")) return

    setSaving(true)
    try {
      await externalLinksAPI.delete(userId, linkId)
      onLinksChange(links.filter(l => l.id !== linkId))
    } catch (error) {
      console.error("Ошибка удаления ссылки:", error)
      alert("Не удалось удалить ссылку: " + (error.message || "Неизвестная ошибка"))
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
