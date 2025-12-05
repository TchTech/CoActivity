"use client"
import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { notificationAPI, userAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/navigation.css"

function Notifications({ onNavigate, currentPage }) {
  const { currentUser } = useUser()
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [unreadCount, setUnreadCount] = useState(0)
  const [showRatingModal, setShowRatingModal] = useState(false)
  const [ratingModalData, setRatingModalData] = useState(null)
  const [ratingValue, setRatingValue] = useState(5)

  useEffect(() => {
    if (!currentUser?.id) {
      setLoading(false)
      return
    }

    const loadNotifications = async () => {
      try {
        const [allNotifications, countData] = await Promise.all([
          notificationAPI.getAll(currentUser.id),
          notificationAPI.getUnreadCount(currentUser.id),
        ])
        setNotifications(Array.isArray(allNotifications) ? allNotifications : [])
        setUnreadCount(countData?.count || 0)
      } catch (error) {
        console.error("Ошибка загрузки уведомлений:", error)
        setNotifications([])
        setUnreadCount(0)
      } finally {
        setLoading(false)
      }
    }

    loadNotifications()
    
    // Poll every 5 seconds for real-time updates
    const interval = setInterval(loadNotifications, 5000)
    
    // Also reload when window gains focus (user switches back to tab)
    const handleFocus = () => {
      loadNotifications()
    }
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        loadNotifications()
      }
    }
    
    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    
    return () => {
      clearInterval(interval)
      window.removeEventListener('focus', handleFocus)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [currentUser])

  const handleMarkAsRead = async (notificationId) => {
    try {
      await notificationAPI.markAsRead(notificationId, currentUser.id)
      setNotifications(notifications.map(n => 
        n.id === notificationId ? { ...n, isRead: true } : n
      ))
      setUnreadCount(Math.max(0, unreadCount - 1))
    } catch (error) {
      console.error("Ошибка отметки уведомления:", error)
    }
  }

  const handleDismiss = async (notificationId) => {
    try {
      await notificationAPI.dismiss(notificationId, currentUser.id)
      setNotifications(notifications.filter(n => n.id !== notificationId))
      if (!notifications.find(n => n.id === notificationId)?.isRead) {
        setUnreadCount(Math.max(0, unreadCount - 1))
      }
    } catch (error) {
      console.error("Ошибка удаления уведомления:", error)
    }
  }

  const handleNotificationClick = (notification) => {
    if (!notification.isRead) {
      handleMarkAsRead(notification.id)
    }

    // Navigate based on notification type
    try {
      const data = notification.data ? JSON.parse(notification.data) : {}
      if (notification.type === "MEMBERSHIP_REQUEST" && data.roomId) {
        onNavigate("roomInfo", data.roomId)
      } else if (notification.type === "MEMBERSHIP_APPROVED" && data.roomId) {
        onNavigate("roomInfo", data.roomId)
      } else if (notification.type === "POST_PINNED" && data.roomId) {
        onNavigate("roomInfo", data.roomId)
      } else if (notification.type === "NEW_POST" && data.postId) {
        onNavigate("comments", data.postId)
      } else if ((notification.type === "POST_LIKED" || notification.type === "POST_DISLIKED" || notification.type === "POST_COMMENTED") && data.postId) {
        onNavigate("comments", data.postId)
      } else if (notification.type === "MENTION" && data.postId) {
        onNavigate("comments", data.postId)
      } else if (notification.type === "MENTION" && data.commentId) {
        // For mentions in comments, navigate to the post
        if (data.postId) {
          onNavigate("comments", data.postId)
        }
      } else if (notification.type === "COMMENT_REPLY" && data.postId) {
        onNavigate("comments", data.postId)
      }
    } catch (e) {
      console.error("Error parsing notification data:", e)
    }
  }

  const formatTime = (timestamp) => {
    if (!timestamp) return ""
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now - date
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return "только что"
    if (diffMins < 60) return `${diffMins} мин. назад`
    if (diffHours < 24) return `${diffHours} ч. назад`
    if (diffDays < 7) return `${diffDays} дн. назад`
    return date.toLocaleDateString("ru-RU")
  }

  return (
    <div>
      <div className="top-nav">
        <div className="top-nav-title">Уведомления</div>
      </div>

      <div style={{ padding: "var(--spacing-md)", paddingBottom: "80px" }}>
        {loading ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            Загрузка...
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            нет уведомлений
          </div>
        ) : (
          notifications.map((notification) => (
            <div
              key={notification.id}
              onClick={() => handleNotificationClick(notification)}
              style={{
                padding: "var(--spacing-md)",
                marginBottom: "var(--spacing-sm)",
                borderBottom: "1px solid var(--border-color)",
                backgroundColor: notification.isRead ? "var(--bg-primary)" : "var(--bg-secondary)",
                cursor: "pointer",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
              }}
            >
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: notification.isRead ? "400" : "600", marginBottom: "var(--spacing-xs)" }}>
                  {notification.title}
                </div>
                <div style={{ fontSize: "var(--font-size-sm)", color: "var(--text-secondary)", marginBottom: "var(--spacing-xs)" }}>
                  {notification.content}
                </div>
                <div style={{ fontSize: "var(--font-size-xs)", color: "var(--text-muted)", marginBottom: "var(--spacing-xs)" }}>
                  {formatTime(notification.createdAt)}
                </div>
                {/* Кнопка "Оценить пользователя" для уведомлений о запросе на оценку */}
                {(notification.type === "RATE_USER_REQUEST" || notification.type === "RATE_ROOM_CREATOR") && (
                  <button
                    className="btn btn-primary"
                    style={{ 
                      fontSize: "var(--font-size-sm)",
                      padding: "var(--spacing-xs) var(--spacing-md)",
                      marginTop: "var(--spacing-xs)"
                    }}
                    onClick={(e) => {
                      e.stopPropagation()
                      try {
                        const data = notification.data ? JSON.parse(notification.data) : {}
                        const userIdToRate = data.requestedUserId || data.creatorId
                        const userName = data.requestedUserName || data.creatorName || "Пользователь"
                        if (userIdToRate && currentUser?.id) {
                          setRatingModalData({
                            userId: userIdToRate,
                            userName: userName,
                            notificationId: notification.id
                          })
                          setRatingValue(5)
                          setShowRatingModal(true)
                        }
                      } catch (error) {
                        console.error("Ошибка при парсинге данных уведомления:", error)
                      }
                    }}
                  >
                    Оценить пользователя
                  </button>
                )}
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-xs)", marginLeft: "var(--spacing-sm)" }}>
                {!notification.isRead && (
                  <div
                    style={{
                      width: "8px",
                      height: "8px",
                      borderRadius: "50%",
                      backgroundColor: "var(--accent-blue)",
                    }}
                  />
                )}
                <button
                  className="btn-icon"
                  onClick={(e) => {
                    e.stopPropagation()
                    handleDismiss(notification.id)
                  }}
                  style={{ padding: "2px", fontSize: "var(--font-size-sm)" }}
                >
                  ×
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      <BottomNavigation currentPage={currentPage || "notifications"} onNavigate={onNavigate} />

      {/* Rating Modal */}
      {showRatingModal && ratingModalData && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0, 0, 0, 0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          onClick={() => setShowRatingModal(false)}
        >
          <div
            style={{
              backgroundColor: "var(--bg-primary)",
              padding: "var(--spacing-lg)",
              borderRadius: "var(--radius-lg)",
              maxWidth: "400px",
              width: "90%",
              boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ marginBottom: "var(--spacing-md)", fontSize: "var(--font-size-lg)" }}>
              Оценить {ratingModalData.userName}
            </h3>
            <div style={{ marginBottom: "var(--spacing-md)" }}>
              <input
                type="range"
                min="0"
                max="10"
                step="0.5"
                value={ratingValue}
                onChange={(e) => setRatingValue(parseFloat(e.target.value))}
                style={{ width: "100%" }}
              />
              <div style={{ textAlign: "center", marginTop: "var(--spacing-sm)", fontSize: "var(--font-size-lg)", fontWeight: "600" }}>
                {ratingValue.toFixed(1)} / 10
              </div>
            </div>
            <div style={{ display: "flex", gap: "var(--spacing-sm)" }}>
              <button 
                className="btn btn-primary" 
                onClick={async () => {
                  try {
                    await userAPI.createRating(ratingModalData.userId, currentUser.id, ratingValue)
                    alert("Оценка сохранена")
                    // Помечаем уведомление как прочитанное
                    if (ratingModalData.notificationId) {
                      const notification = notifications.find(n => n.id === ratingModalData.notificationId)
                      if (notification && !notification.isRead) {
                        handleMarkAsRead(ratingModalData.notificationId)
                      }
                    }
                    setShowRatingModal(false)
                    setRatingModalData(null)
                  } catch (error) {
                    console.error("Ошибка при оценке пользователя:", error)
                    alert("Не удалось сохранить оценку: " + (error.message || "Неизвестная ошибка"))
                  }
                }} 
                style={{ flex: 1 }}
              >
                Сохранить
              </button>
              <button 
                className="btn btn-secondary" 
                onClick={() => {
                  setShowRatingModal(false)
                  setRatingModalData(null)
                }} 
                style={{ flex: 1 }}
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Notifications

