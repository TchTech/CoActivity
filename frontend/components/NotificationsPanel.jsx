"use client"

import { useState, useEffect } from "react"
import { notificationAPI } from "../lib/api"
import { parseNotificationData, isMembershipRequestType, isMembershipDecisionType } from "../types"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"

function NotificationsPanel({ userId, onClose, onNavigate }) {
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
    const interval = setInterval(loadNotifications, 30000) // Poll every 30s
    return () => clearInterval(interval)
  }, [userId])

  const handleMarkAsRead = async (notificationId) => {
    try {
      await notificationAPI.markAsRead(notificationId, userId)
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
      await notificationAPI.dismiss(notificationId, userId)
      setNotifications(notifications.filter(n => n.id !== notificationId))
      if (!notifications.find(n => n.id === notificationId)?.isRead) {
        setUnreadCount(Math.max(0, unreadCount - 1))
      }
    } catch (error) {
      console.error("Ошибка удаления уведомления:", error)
    }
  }

  const handleMarkAllAsRead = async () => {
    try {
      await notificationAPI.markAllAsRead(userId)
      setNotifications(notifications.map(n => ({ ...n, isRead: true })))
      setUnreadCount(0)
    } catch (error) {
      console.error("Ошибка отметки всех уведомлений:", error)
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
      style={{
        position: "fixed",
        top: "60px",
        right: "var(--spacing-md)",
        width: "350px",
        maxHeight: "500px",
        backgroundColor: "var(--bg-primary)",
        border: "1px solid var(--border-color)",
        borderRadius: "var(--radius-lg)",
        boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
        zIndex: 1000,
        display: "flex",
        flexDirection: "column",
      }}
    >
      <div
        style={{
          padding: "var(--spacing-md)",
          borderBottom: "1px solid var(--border-color)",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <h3 style={{ margin: 0, fontSize: "var(--font-size-lg)" }}>Уведомления</h3>
        <div style={{ display: "flex", gap: "var(--spacing-sm)", alignItems: "center" }}>
          {unreadCount > 0 && (
            <button
              className="btn btn-sm"
              onClick={handleMarkAllAsRead}
              style={{ fontSize: "var(--font-size-sm)", padding: "4px 8px" }}
            >
              Отметить все прочитанными
            </button>
          )}
          <button
            className="btn-icon"
            onClick={onClose}
            style={{ padding: "4px" }}
          >
            ×
          </button>
        </div>
      </div>

      <div style={{ overflowY: "auto", flex: 1 }}>
        {loading ? (
          <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--text-muted)" }}>
            Загрузка...
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--text-muted)" }}>
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
    </div>
  )
}

export default NotificationsPanel

