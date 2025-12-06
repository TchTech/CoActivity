"use client"

import { useState, useEffect } from "react"
import { notificationAPI } from "../lib/api"
import { parseNotificationData, isMembershipRequestType, isMembershipDecisionType } from "../types"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"

function NotificationsPanel({ userId, onClose, onNavigate, onNotificationUpdate }) {
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!userId) {
      setLoading(false)
      return
    }

    const loadNotifications = async () => {
      try {
        const [allNotifications, countData] = await Promise.all([
          notificationAPI.getAll(userId),
          notificationAPI.getUnreadCount(userId),
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
    window.addEventListener('focus', handleFocus)
    
    return () => {
      clearInterval(interval)
      window.removeEventListener('focus', handleFocus)
    }
  }, [userId])

  const handleMarkAsRead = async (notificationId) => {
    try {
      await notificationAPI.markAsRead(notificationId, userId)
      setNotifications(notifications.map(n => 
        n.id === notificationId ? { ...n, isRead: true } : n
      ))
      setUnreadCount(Math.max(0, unreadCount - 1))
      // Notify parent to refresh badge counter
      if (onNotificationUpdate) {
        onNotificationUpdate()
      }
    } catch (error) {
      console.error("Ошибка отметки уведомления:", error)
    }
  }

  const handleDismiss = async (notificationId) => {
    try {
      const notification = notifications.find(n => n.id === notificationId)
      await notificationAPI.dismiss(notificationId, userId)
      setNotifications(notifications.filter(n => n.id !== notificationId))
      if (notification && !notification.isRead) {
        setUnreadCount(Math.max(0, unreadCount - 1))
      }
      // Notify parent to refresh badge counter
      if (onNotificationUpdate) {
        onNotificationUpdate()
      }
    } catch (error) {
      console.error("Ошибка удаления уведомления:", error)
    }
  }

  const handleMarkAllAsRead = async () => {
    if (!userId) {
      console.error("Cannot mark all as read: userId is missing")
      return
    }
    
    try {
      console.log("[NotificationsPanel] Marking all notifications as read for user:", userId)
      await notificationAPI.markAllAsRead(userId)
      console.log("[NotificationsPanel] Successfully marked all as read")
      
      // Update local state
      setNotifications(notifications.map(n => ({ ...n, isRead: true })))
      setUnreadCount(0)
      
      // Notify parent to refresh badge counter
      if (onNotificationUpdate) {
        onNotificationUpdate()
      }
    } catch (error) {
      console.error("Ошибка отметки всех уведомлений:", error)
      console.error("Не удалось отметить все уведомления как прочитанные:", error)
    }
  }

  const handleNotificationClick = (notification) => {
    if (!notification.isRead) {
      handleMarkAsRead(notification.id)
    }

    // Navigate based on notification type using parseNotificationData
    const data = parseNotificationData(notification)
    
    if (isMembershipRequestType(notification.type) && data?.roomId) {
      // For membership requests, navigate to room with requests tab
      onNavigate("roomInfo", data.roomId)
    } else if (isMembershipDecisionType(notification.type) && data?.roomId) {
      // For membership decisions (approved/rejected), navigate to room
      onNavigate("roomInfo", data.roomId)
    } else if (notification.type === "POST_PINNED" && data?.roomId) {
      onNavigate("roomInfo", data.roomId)
    } else if (notification.type === "COMMENT" && data?.roomId) {
      // Navigate to post comments if available
      if (data.postId) {
        onNavigate("comments", data.postId)
      } else if (data.roomId) {
        onNavigate("roomInfo", data.roomId)
      }
    } else if (notification.type === "MENTION" && data?.roomId) {
      // Navigate to the mentioned post or room
      if (data.postId) {
        onNavigate("comments", data.postId)
      } else if (data.roomId) {
        onNavigate("roomInfo", data.roomId)
      }
    } else if (notification.type === "NEW_POST" && data?.postId) {
      onNavigate("comments", data.postId)
    } else if ((notification.type === "POST_LIKED" || notification.type === "POST_DISLIKED" || notification.type === "POST_COMMENTED") && data?.postId) {
      onNavigate("comments", data.postId)
    } else if (notification.type === "COMMENT_REPLY" && data?.postId) {
      onNavigate("comments", data.postId)
    }
    
    onClose()
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
    <div
      className="card"
      style={{
        width: "100%",
        maxHeight: "500px",
        display: "flex",
        flexDirection: "column",
        padding: 0,
        borderRadius: "var(--radius-lg)",
        boxShadow: "var(--shadow-lg)",
      }}
    >
      <div
        style={{
          padding: "var(--spacing-md)",
          borderBottom: "1px solid var(--border-color)",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          backgroundColor: "var(--bg-secondary)",
          borderTopLeftRadius: "var(--radius-lg)",
          borderTopRightRadius: "var(--radius-lg)",
        }}
      >
        <h3 style={{ 
          margin: 0, 
          fontSize: "var(--font-size-lg)",
          fontWeight: "600",
          color: "var(--accent-gold)"
        }}>
          Уведомления
        </h3>
        <button
          className="btn-icon"
          onClick={onClose}
          style={{ 
            padding: "4px",
            width: "32px",
            height: "32px",
            fontSize: "var(--font-size-lg)"
          }}
        >
          ×
        </button>
      </div>

      <div style={{ 
        overflowY: "auto", 
        flex: 1,
        backgroundColor: "var(--bg-primary)"
      }}>
        {loading ? (
          <div style={{ 
            padding: "var(--spacing-lg)", 
            textAlign: "center", 
            color: "var(--text-muted)" 
          }}>
            Загрузка...
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ 
            padding: "var(--spacing-lg)", 
            textAlign: "center", 
            color: "var(--text-muted)" 
          }}>
            Нет уведомлений
          </div>
        ) : (
          notifications.map((notification) => (
            <div
              key={notification.id}
              onClick={() => handleNotificationClick(notification)}
              style={{
                padding: "var(--spacing-md)",
                borderBottom: "1px solid var(--border-color)",
                backgroundColor: notification.isRead ? "var(--bg-primary)" : "var(--bg-secondary)",
                cursor: "pointer",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
                transition: "all 0.2s ease",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = "var(--bg-hover)"
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = notification.isRead ? "var(--bg-primary)" : "var(--bg-secondary)"
              }}
            >
              <div style={{ flex: 1 }}>
                <div style={{ 
                  fontWeight: notification.isRead ? "400" : "600", 
                  marginBottom: "var(--spacing-xs)",
                  color: notification.isRead ? "var(--text-secondary)" : "var(--text-primary)"
                }}>
                  {notification.title}
                </div>
                <div style={{ 
                  fontSize: "var(--font-size-sm)", 
                  color: "var(--text-secondary)", 
                  marginBottom: "var(--spacing-xs)",
                  lineHeight: "1.5"
                }}>
                  {notification.content}
                </div>
                <div style={{ 
                  fontSize: "var(--font-size-xs)", 
                  color: "var(--text-muted)" 
                }}>
                  {formatTime(notification.createdAt)}
                </div>
              </div>
              <div style={{ 
                display: "flex", 
                flexDirection: "column", 
                gap: "var(--spacing-xs)", 
                marginLeft: "var(--spacing-sm)",
                alignItems: "center"
              }}>
                {!notification.isRead && (
                  <div
                    style={{
                      width: "10px",
                      height: "10px",
                      borderRadius: "50%",
                      backgroundColor: "var(--accent-gold)",
                      boxShadow: "0 0 8px rgba(212, 175, 55, 0.6)",
                    }}
                  />
                )}
                <button
                  className="btn-icon"
                  onClick={(e) => {
                    e.stopPropagation()
                    handleDismiss(notification.id)
                  }}
                  style={{ 
                    padding: "4px", 
                    fontSize: "var(--font-size-base)",
                    width: "28px",
                    height: "28px",
                    opacity: 0.7
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.opacity = "1"
                    e.currentTarget.style.color = "var(--error-color)"
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.opacity = "0.7"
                    e.currentTarget.style.color = "var(--text-primary)"
                  }}
                >
                  ×
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default NotificationsPanel

