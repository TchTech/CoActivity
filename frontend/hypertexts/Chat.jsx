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
  const [roomMembers, setRoomMembers] = useState([]) // Список участников для упоминаний
  const [roomCreator, setRoomCreator] = useState(null) // Информация о создателе комнаты
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [isAdmin, setIsAdmin] = useState(false)

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
            ? data.messages.map((m) => {
                console.log("[Chat] Mapping message:", m, "senderAvatar:", m.senderAvatar)
                if (!m.id) {
                  console.warn("[Chat] Message missing ID:", m)
                }
                return {
                  id: m.id,
                  senderId: m.senderId,
                  senderName: m.senderName,
                  senderAvatar: m.senderAvatar,
                  content: m.content,
                  timestamp: m.timestamp,
                }
              })
            : []
        )
        
        // Загружаем детали комнаты для участников и создателя
        try {
          const roomDetails = await roomAPI.getDetails(roomId)
          console.log("[Chat] Room details:", roomDetails)
          console.log("[Chat] Creator ID:", roomDetails.creatorId)
          console.log("[Chat] Creator name:", roomDetails.creatorName)
          console.log("[Chat] Creator avatar:", roomDetails.creatorAvatar)
          console.log("[Chat] Members:", roomDetails.members)
          
          if (roomDetails.members && Array.isArray(roomDetails.members)) {
            setRoomMembers(roomDetails.members)
          }
          // Сохраняем информацию о создателе
          if (roomDetails.creatorId) {
            // Находим создателя в списке участников (сравниваем как числа)
            const creator = roomDetails.members?.find(m => {
              if (!m || !m.id) return false
              // Сравниваем ID как числа, учитывая возможные различия в типах
              const memberId = Number(m.id)
              const creatorId = Number(roomDetails.creatorId)
              return memberId === creatorId
            })
            console.log("[Chat] Found creator in members:", creator)
            
            if (creator) {
              console.log("[Chat] Setting creator from members:", creator)
              setRoomCreator(creator)
            } else {
              // Если создателя нет в списке участников, создаем объект из данных комнаты
              // Важно: creatorAvatar имеет структуру { id: Integer }
              const creatorData = {
                id: roomDetails.creatorId,
                name: roomDetails.creatorName,
                username: roomDetails.creatorName,
                avatar: roomDetails.creatorAvatar || null
              }
              console.log("[Chat] Creator not found in members, using room details")
              console.log("[Chat] Setting creator from room details:", creatorData)
              console.log("[Chat] creatorAvatar structure:", roomDetails.creatorAvatar)
              setRoomCreator(creatorData)
            }
          } else {
            console.log("[Chat] No creatorId in room details")
          }
        } catch (err) {
          console.error("[Chat] Error loading room details:", err)
        }
        
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

  // Load room details to check admin status
  useEffect(() => {
    const loadRoomDetails = async () => {
      if (!roomId || !currentUser?.id) return

      try {
        const roomData = await roomAPI.getDetails(roomId)
        const currentUserMember = roomData.members?.find(m => m.id === currentUser.id)
        setIsAdmin(currentUserMember?.isAdmin || false)
      } catch (err) {
        console.error("Ошибка загрузки информации о комнате:", err)
      }
    }

    loadRoomDetails()
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

  // Функция для парсинга упоминаний в тексте сообщения
  const parseMessageWithMentions = (text, members, navigate) => {
    if (!text || !members || members.length === 0) {
      return text
    }

    // Регулярное выражение для поиска упоминаний @username
    const mentionRegex = /@(\w+)/g
    const parts = []
    let lastIndex = 0
    let match

    while ((match = mentionRegex.exec(text)) !== null) {
      // Добавляем текст до упоминания
      if (match.index > lastIndex) {
        parts.push(text.substring(lastIndex, match.index))
      }

      const mentionedUsername = match[1]
      // Ищем пользователя в списке участников
      const mentionedUser = members.find(
        (member) =>
          member.username?.toLowerCase() === mentionedUsername.toLowerCase() ||
          member.name?.toLowerCase() === mentionedUsername.toLowerCase()
      )

      if (mentionedUser) {
        // Создаем кликабельную ссылку на профиль
        parts.push(
          <span
            key={match.index}
            onClick={(e) => {
              e.stopPropagation()
              if (mentionedUser.id) {
                navigate("profile", mentionedUser.id)
              }
            }}
            style={{
              color: "var(--accent-gold)",
              cursor: "pointer",
              fontWeight: "600",
              textDecoration: "underline",
              textDecorationColor: "var(--accent-gold)",
              textUnderlineOffset: "2px"
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.opacity = "0.8"
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.opacity = "1"
            }}
          >
            @{mentionedUser.name || mentionedUser.username}
          </span>
        )
      } else {
        // Если пользователь не найден, оставляем как обычный текст
        parts.push(`@${mentionedUsername}`)
      }

      lastIndex = match.index + match[0].length
    }

    // Добавляем оставшийся текст
    if (lastIndex < text.length) {
      parts.push(text.substring(lastIndex))
    }

    return parts.length > 0 ? parts : text
  }

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
        {(() => {
          // Детальная проверка наличия аватарки
          const avatarId = roomCreator?.avatar?.id
          const hasAvatar = avatarId != null && avatarId !== undefined && avatarId !== 0
          
          console.log("[Chat] Rendering header avatar")
          console.log("[Chat] roomCreator:", roomCreator)
          console.log("[Chat] roomCreator?.avatar:", roomCreator?.avatar)
          console.log("[Chat] avatarId:", avatarId)
          console.log("[Chat] hasAvatar:", hasAvatar)
          
          if (hasAvatar) {
            const imageUrl = imageAPI.getImageUrl(avatarId)
            console.log("[Chat] Avatar image URL:", imageUrl)
            return (
              <img 
                src={imageUrl} 
                alt={roomCreator.name || roomCreator.username || headerTitle} 
                className="avatar avatar-md avatar-clickable"
                onClick={(e) => {
                  e.stopPropagation()
                  if (roomCreator.id) {
                    onNavigate("profile", roomCreator.id)
                  }
                }}
                onError={(e) => {
                  console.error("[Chat] Failed to load avatar image:", imageUrl)
                  e.target.style.display = 'none'
                }}
              />
            )
          } else {
            console.log("[Chat] No avatar, rendering empty div")
            return (
              <div
                className="avatar avatar-md"
                style={{
                  backgroundColor: "transparent",
                  border: "none",
                  width: "40px",
                  height: "40px"
                }}
              />
            )
          }
        })()}
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
                  <div style={{ display: "flex", alignItems: "center", gap: "var(--spacing-xs)", justifyContent: "space-between" }}>
                    <div style={{ flex: 1 }}>
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
                    {isAdmin && message.id && (
                      <button
                        className="btn-icon"
                        style={{ 
                          fontSize: "var(--font-size-xs)",
                          padding: "var(--spacing-xs)",
                          color: "var(--error)",
                          opacity: 0.7
                        }}
                        onClick={async (e) => {
                          e.stopPropagation()
                          if (confirm("Удалить это сообщение?")) {
                            try {
                              if (!message.id) {
                                alert("Не удалось удалить сообщение: отсутствует ID сообщения")
                                return
                              }
                              await roomAPI.deleteMessage(roomId, message.id, currentUser.id)
                              // Remove message from local state
                              setMessages((prev) => prev.filter((m) => m.id !== message.id))
                            } catch (err) {
                              console.error("Ошибка удаления сообщения:", err)
                              alert("Не удалось удалить сообщение: " + (err.message || "Неизвестная ошибка"))
                            }
                          }
                        }}
                        title="Удалить сообщение"
                      >
                        ✕
                      </button>
                    )}
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
