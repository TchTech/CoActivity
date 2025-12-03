"use client"
import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { notificationAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/navigation.css"

function Notifications({ onNavigate }) {
  const { currentUser } = useUser()
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [unreadCount, setUnreadCount] = useState(0)

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
    const interval = setInterval(loadNotifications, 30000) // Poll every 30s
    return () => clearInterval(interval)
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
                <div style={{ fontSize: "var(--font-size-xs)", color: "var(--text-muted)" }}>
                  {formatTime(notification.createdAt)}
                </div>
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

      <BottomNavigation currentPage="notifications" onNavigate={onNavigate} />
    </div>
  )
}

export default Notifications

