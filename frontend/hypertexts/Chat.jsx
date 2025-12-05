"use client"

import { useEffect, useState } from "react"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"
import { roomAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"

function Chat({ onNavigate, roomId }) {
  const { currentUser } = useUser()
  const [messages, setMessages] = useState([])
  const [messageText, setMessageText] = useState("")
  const [roomInfo, setRoomInfo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  // Polling for messages (fallback instead of websockets)
  useEffect(() => {
    let intervalId

    const loadChat = async () => {
      if (!roomId || !currentUser?.id) {
        setLoading(false)
        return
      }

      try {
        const data = await roomAPI.openChat(roomId, currentUser.id)
        console.log("[Chat] Received data from API:", data)
        console.log("[Chat] Messages:", data.messages)
        setRoomInfo({ id: data.roomId })
        setMessages(
          Array.isArray(data.messages)
            ? data.messages.map((m, index) => {
                console.log("[Chat] Mapping message:", m, "senderAvatar:", m.senderAvatar)
                return {
                  id: m.id || index,
                  senderId: m.senderId,
                  senderName: m.senderName,
                  senderAvatar: m.senderAvatar,
                  content: m.content,
                  timestamp: m.timestamp,
                }
              })
            : []
        )
        setError("")
      } catch (err) {
        console.error("Ошибка загрузки чата:", err)
        // If 403, user is not a member - show helpful message
        if (err.status === 403) {
          setError("Вы не являетесь участником этой комнаты. Присоединитесь к комнате, чтобы видеть сообщения.")
        } else {
          setError(err.message || "Не удалось загрузить чат")
        }
      } finally {
        setLoading(false)
      }
    }

    loadChat()
    intervalId = setInterval(loadChat, 5000)

    return () => {
      if (intervalId) clearInterval(intervalId)
    }
  }, [roomId, currentUser])

  const handleSendMessage = async () => {
    if (!messageText.trim() || !currentUser?.id || !roomId) return

    const text = messageText
    setMessageText("")

    try {
      await roomAPI.sendMessage(roomId, currentUser.id, text)
      // Optimistically append message
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now(),
          senderId: currentUser.id,
          senderName: currentUser.name || currentUser.username,
          senderAvatar: currentUser.avatar,
          content: text,
          timestamp: new Date().toISOString(),
        },
      ])
    } catch (err) {
      console.error("Ошибка отправки сообщения:", err)
      setError(err.message || "Не удалось отправить сообщение")
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  const headerTitle = roomInfo?.description || `Комната #${roomId || ""}`

  return (
    <div className="chat-container">
      {/* Шапка чата - кликабельна */}
      <div className="chat-header" onClick={() => onNavigate("roomInfo", roomId)}>
        <button
          className="btn-icon"
          onClick={(e) => {
            e.stopPropagation()
            onNavigate("rooms")
          }}
        >
          ←
        </button>
        <img src="/placeholder.svg" alt={headerTitle} className="avatar avatar-md" />
        <div className="chat-header-info">
          <div className="chat-header-title">{headerTitle}</div>
        </div>
        <button className="btn-icon" onClick={(e) => e.stopPropagation()}>
          ⋮
        </button>
      </div>

      {/* Сообщения */}
      <div className="chat-messages">
        {loading ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            Загрузка сообщений...
          </div>
        ) : error ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--error-color)" }}>
            {error}
          </div>
        ) : messages.length === 0 ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            Пока нет сообщений
          </div>
        ) : (
          messages.map((message) => {
            const isOwn = currentUser && message.senderId === currentUser.id
            const senderName = message.senderName || `Участник #${message.senderId}`
            // Проверяем наличие аватарки: либо из данных сообщения, либо из currentUser для собственных сообщений
            let avatarId = null
            if (message.senderAvatar && message.senderAvatar.id !== null && message.senderAvatar.id !== undefined) {
              avatarId = message.senderAvatar.id
            } else if (isOwn && currentUser && currentUser.avatar && currentUser.avatar.id) {
              avatarId = currentUser.avatar.id
            }
            const hasAvatar = avatarId !== null && avatarId !== undefined
            console.log("[Chat] Rendering message:", message, "hasAvatar:", hasAvatar, "avatarId:", avatarId, "isOwn:", isOwn)
            
            return (
              <div key={message.id} className={`message ${isOwn ? "own" : ""}`}>
                {hasAvatar ? (
                  <img 
                    src={imageAPI.getImageUrl(avatarId)} 
                    alt={senderName} 
                    className="avatar avatar-md avatar-clickable"
                    onClick={(e) => {
                      e.stopPropagation()
                      if (message.senderId) {
                        onNavigate("profile", message.senderId)
                      }
                    }}
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
                <div className="message-content">
                  {!isOwn && <div className="message-sender">{senderName}</div>}
                  <div className="message-text">{message.content}</div>
                  <div className="message-time">
                    {message.timestamp
                      ? new Date(message.timestamp).toLocaleTimeString("ru-RU", {
                          hour: "2-digit",
                          minute: "2-digit",
                        })
                      : ""}
                  </div>
                </div>
              </div>
            )
          })
        )}
      </div>

      {/* Поле ввода */}
      <div className="chat-input-container">
        <button className="btn-icon">📎</button>
        <textarea
          className="chat-input"
          placeholder="Сообщение (до 500 символов)..."
          value={messageText}
          onChange={(e) => setMessageText(e.target.value)}
          onKeyPress={handleKeyPress}
          maxLength="500"
          rows="1"
        />
        <button className="chat-send-btn" onClick={handleSendMessage}>
          ➤
        </button>
      </div>
    </div>
  )
}

export default Chat
