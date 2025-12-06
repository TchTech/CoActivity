"use client"

import { useState, useEffect } from "react"
import { roomAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { AlertDialog } from "./ui/AlertDialog"
import { ConfirmDialog } from "./ui/ConfirmDialog"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"

/**
 * Компонент кнопки присоединения к комнате из поста
 * Показывает "Присоединиться" для открытых комнат или "Запросить вступление" для комнат по заявкам
 */
export default function PostRoomJoinButton({ post, onNavigate }) {
  const { currentUser } = useUser()
  const [loading, setLoading] = useState(false)
  const [roomInfo, setRoomInfo] = useState(null)
  const [error, setError] = useState(null)
  const [isMember, setIsMember] = useState(false)
  const [hasPendingRequest, setHasPendingRequest] = useState(false)
  const [pendingRequestId, setPendingRequestId] = useState(null)
  const [isLoadingRoomInfo, setIsLoadingRoomInfo] = useState(true)
  const [showAlert, setShowAlert] = useState(false)
  const [alertData, setAlertData] = useState({ title: "", message: "", variant: "info" })
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)

  // Получаем ID комнаты из поста - проверяем разные варианты
  const getRoomId = () => {
    if (!post.room) {
      return post.roomId || null
    }
    
    // Если room - это объект
    if (typeof post.room === "object" && post.room !== null) {
      return post.room.id || post.room.roomId || post.roomId || null
    }
    
    // Если room - это число или строка
    if (typeof post.room === "number" || typeof post.room === "string") {
      return post.room
    }
    
    return post.roomId || null
  }
  
  const roomId = getRoomId()

  // Логирование для отладки
  useEffect(() => {
    console.log("[PostRoomJoinButton] Post:", post)
    console.log("[PostRoomJoinButton] Room ID:", roomId)
    console.log("[PostRoomJoinButton] Current User:", currentUser)
  }, [post, roomId, currentUser])

  // Загружаем информацию о комнате
  useEffect(() => {
    console.log("[PostRoomJoinButton] useEffect triggered:", { roomId, currentUserId: currentUser?.id })
    
    if (!roomId || !currentUser?.id) {
      console.log("[PostRoomJoinButton] Missing roomId or currentUser, skipping load")
      setIsLoadingRoomInfo(false)
      return
    }

    const loadRoomInfo = async () => {
      setIsLoadingRoomInfo(true)
      console.log("[PostRoomJoinButton] Starting to load room details for roomId:", roomId)
      try {
        const details = await roomAPI.getDetails(roomId)
        console.log("[PostRoomJoinButton] Room details loaded successfully:", details)
        console.log("[PostRoomJoinButton] Room joinType:", details.joinType)
        console.log("[PostRoomJoinButton] Room members:", details.members)
        setRoomInfo(details)
        
        // Проверяем, является ли пользователь участником
        const members = details.members || []
        console.log("[PostRoomJoinButton] Checking membership. Members:", members)
        console.log("[PostRoomJoinButton] Current user ID:", currentUser.id)
        const isInRoom = members.some(m => {
          const memberId = m.id || m.userId
          const matches = memberId === currentUser.id
          console.log("[PostRoomJoinButton] Comparing member:", memberId, "with user:", currentUser.id, "matches:", matches)
          return matches
        })
        console.log("[PostRoomJoinButton] User is member:", isInRoom)
        setIsMember(isInRoom)

        // Проверяем наличие ожидающей заявки пользователя
        if (details.joinType === "by_application" || details.joinType === "REQUEST_ONLY") {
          console.log("[PostRoomJoinButton] Room requires application, checking for pending requests")
          try {
            const userRequests = await roomAPI.getMyPendingRequests(currentUser.id)
            console.log("[PostRoomJoinButton] User requests:", userRequests)
            const userRequest = Array.isArray(userRequests) 
              ? userRequests.find(r => {
                  const reqRoomId = r.roomId || (r.room && (r.room.id || r.room))
                  const matches = reqRoomId === roomId && r.status === "pending"
                  console.log("[PostRoomJoinButton] Checking request:", { reqRoomId, roomId, status: r.status, matches })
                  return matches
                })
              : null
            console.log("[PostRoomJoinButton] Found pending request:", !!userRequest)
            setHasPendingRequest(!!userRequest)
            if (userRequest) {
              setPendingRequestId(userRequest.id)
            }
          } catch (err) {
            console.error("[PostRoomJoinButton] Ошибка проверки заявок пользователя:", err)
            // Если не удалось проверить, просто не показываем статус заявки
          }
        } else {
          console.log("[PostRoomJoinButton] Room is open type, no need to check requests")
        }
      } catch (err) {
        console.error("[PostRoomJoinButton] Ошибка загрузки информации о комнате:", err)
        console.error("[PostRoomJoinButton] Error details:", {
          message: err.message,
          status: err.status,
          stack: err.stack
        })
        setError("Не удалось загрузить информацию о комнате: " + (err.message || "Неизвестная ошибка"))
      } finally {
        setIsLoadingRoomInfo(false)
        console.log("[PostRoomJoinButton] Finished loading room info")
      }
    }

    loadRoomInfo()
  }, [roomId, currentUser?.id])

  // Если нет комнаты или пользователя, не показываем кнопку
  if (!roomId || !currentUser) {
    console.log("[PostRoomJoinButton] No roomId or currentUser:", { roomId, currentUser: !!currentUser })
    return null
  }

  // Показываем индикатор загрузки, пока загружаем информацию о комнате
  if (isLoadingRoomInfo) {
    console.log("[PostRoomJoinButton] Loading room info...")
    return (
      <div style={{ marginTop: "var(--spacing-sm)", marginBottom: "var(--spacing-sm)" }}>
        <div style={{ 
          textAlign: "center", 
          padding: "var(--spacing-sm)", 
          color: "var(--text-muted)",
          fontSize: "var(--font-size-sm)"
        }}>
          Загрузка...
        </div>
      </div>
    )
  }

  // Если информация о комнате не загружена (ошибка), не показываем кнопку
  if (!roomInfo) {
    console.log("[PostRoomJoinButton] Room info not loaded, error:", error)
    if (error) {
      return (
        <div style={{ marginTop: "var(--spacing-sm)", marginBottom: "var(--spacing-sm)" }}>
          <div style={{ 
            fontSize: "var(--font-size-sm)", 
            color: "var(--error-color)" 
          }}>
            {error}
          </div>
        </div>
      )
    }
    return null
  }

  // Проверяем, является ли пользователь владельцем комнаты
  const isCreator = roomInfo.creatorId === currentUser.id
  
  // Обработчик открытия чата
  const handleOpenChat = (e) => {
    e.stopPropagation()
    if (roomId && onNavigate) {
      onNavigate("chat", roomId)
    }
  }

  // Получаем ID комнаты для навигации
  const getRoomIdForNavigation = () => {
    if (typeof post.room === "object" && post.room !== null) {
      return post.room?.id || post.roomId || null
    }
    return post.roomId || null
  }

  // Получаем имя комнаты для метки
  const getRoomName = () => {
    if (typeof post.room === "object" && post.room !== null) {
      return post.room?.name || "Комната"
    }
    return "Комната"
  }

  const roomIdForNav = getRoomIdForNavigation()
  const roomName = getRoomName()
  
  // Обработчик клика на метку комнаты
  const handleRoomLabelClick = (e) => {
    e.stopPropagation()
    if (roomIdForNav && onNavigate) {
      onNavigate("roomInfo", roomIdForNav)
    }
  }

  // Если пользователь уже участник или владелец, показываем обе кнопки рядом
  if (isMember || isCreator) {
    console.log("[PostRoomJoinButton] User is member or creator, showing chat button:", { isMember, isCreator })
    
    return (
      <div style={{ 
        marginTop: "var(--spacing-sm)", 
        marginBottom: "var(--spacing-sm)",
        display: "flex",
        alignItems: "center",
        gap: "var(--spacing-sm)",
        flexWrap: "wrap"
      }}>
        {/* Кнопка метки комнаты - овальная, желтая outline */}
        <button
          onClick={handleRoomLabelClick}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "var(--spacing-xs)",
            padding: "var(--spacing-xs) var(--spacing-md)",
            borderRadius: "20px", // Овальная форма
            border: "2px solid var(--accent-gold)",
            backgroundColor: "transparent",
            color: "var(--accent-gold)",
            fontSize: "var(--font-size-sm)",
            fontWeight: "500",
            cursor: "pointer",
            transition: "all 0.2s ease",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = "var(--accent-gold)"
            e.currentTarget.style.color = "var(--bg-primary)"
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = "transparent"
            e.currentTarget.style.color = "var(--accent-gold)"
          }}
        >
          {/* Иконка двери (вход в комнату) */}
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
            <polyline points="10 17 15 12 10 7" />
            <line x1="15" y1="12" x2="3" y2="12" />
          </svg>
          <span>{roomName}</span>
        </button>

        {/* Кнопка открыть чат - серая с иконкой сообщений */}
        <button
          onClick={handleOpenChat}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "var(--spacing-xs)",
            padding: "var(--spacing-xs) var(--spacing-md)",
            borderRadius: "var(--radius-md)",
            border: "1px solid var(--border-color)",
            backgroundColor: "var(--bg-tertiary)",
            color: "var(--text-secondary)",
            fontSize: "var(--font-size-sm)",
            fontWeight: "500",
            cursor: "pointer",
            transition: "all 0.2s ease",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = "var(--bg-secondary)"
            e.currentTarget.style.borderColor = "var(--text-muted)"
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = "var(--bg-tertiary)"
            e.currentTarget.style.borderColor = "var(--border-color)"
          }}
        >
          {/* Иконка сообщений */}
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <span>Чат</span>
        </button>
      </div>
    )
  }

  const joinType = roomInfo.joinType || "open"
  const isOpenRoom = joinType === "open"
  const isRequestRoom = joinType === "by_application" || joinType === "REQUEST_ONLY"
  
  console.log("[PostRoomJoinButton] Room info:", {
    joinType,
    isOpenRoom,
    isRequestRoom,
    hasPendingRequest,
    isMember,
    isCreator
  })

  // Обработчик присоединения к открытой комнате
  const handleJoinOpenRoom = async () => {
    if (!currentUser?.id || !roomId) return

    setLoading(true)
    setError(null)

    try {
      await roomAPI.joinRoom(roomId, currentUser.id)
      setIsMember(true)
      // Перезагружаем информацию о комнате для обновления состояния
      const details = await roomAPI.getDetails(roomId)
      setRoomInfo(details)
      const members = details.members || []
      const isInRoom = members.some(m => (m.id || m.userId) === currentUser.id)
      setIsMember(isInRoom)
    } catch (err) {
      const errorMsg = err.message || "Не удалось присоединиться к комнате"
      setError(errorMsg)
      setAlertData({ title: "Ошибка", message: errorMsg, variant: "error" })
      setShowAlert(true)
    } finally {
      setLoading(false)
    }
  }

  // Обработчик отправки заявки на вступление
  const handleRequestJoin = async () => {
    if (!currentUser?.id || !roomId || hasPendingRequest) return

    setLoading(true)
    setError(null)

    try {
      const request = await roomAPI.createJoinRequest(roomId, currentUser.id, {})
      setHasPendingRequest(true)
      if (request && request.id) {
        setPendingRequestId(request.id)
      }
      setAlertData({ title: "Успешно", message: "Заявка на вступление отправлена!", variant: "success" })
      setShowAlert(true)
      // Перезагружаем информацию о комнате
      const details = await roomAPI.getDetails(roomId)
      setRoomInfo(details)
    } catch (err) {
      const errorMsg = err.message || "Не удалось отправить заявку"
      setError(errorMsg)
      setAlertData({ title: "Ошибка", message: errorMsg, variant: "error" })
      setShowAlert(true)
    } finally {
      setLoading(false)
    }
  }

  // Обработчик отмены присоединения к открытой комнате
  const handleLeaveOpenRoom = () => {
    if (!currentUser?.id || !roomId) return
    setShowLeaveConfirm(true)
  }

  const confirmLeaveOpenRoom = async () => {
    setShowLeaveConfirm(false)
    setLoading(true)
    setError(null)

    try {
      await roomAPI.removeUserFromRoom(roomId, currentUser.id)
      setIsMember(false)
      setAlertData({ title: "Успешно", message: "Вы покинули комнату", variant: "success" })
      setShowAlert(true)
      // Перезагружаем информацию о комнате
      const details = await roomAPI.getDetails(roomId)
      setRoomInfo(details)
      const members = details.members || []
      const isInRoom = members.some(m => (m.id || m.userId) === currentUser.id)
      setIsMember(isInRoom)
    } catch (err) {
      const errorMsg = err.message || "Не удалось покинуть комнату"
      setError(errorMsg)
      setAlertData({ title: "Ошибка", message: errorMsg, variant: "error" })
      setShowAlert(true)
    } finally {
      setLoading(false)
    }
  }

  // Обработчик отзыва заявки на вступление
  const handleCancelRequest = () => {
    if (!currentUser?.id || !roomId || !pendingRequestId) return
    setShowCancelConfirm(true)
  }

  const confirmCancelRequest = async () => {
    setShowCancelConfirm(false)
    setLoading(true)
    setError(null)

    try {
      await roomAPI.cancelRequest(roomId, pendingRequestId, currentUser.id)
      setHasPendingRequest(false)
      setPendingRequestId(null)
      setAlertData({ title: "Успешно", message: "Заявка отозвана", variant: "success" })
      setShowAlert(true)
      // Перезагружаем информацию о заявках
      try {
        const userRequests = await roomAPI.getMyPendingRequests(currentUser.id)
        const userRequest = Array.isArray(userRequests) 
          ? userRequests.find(r => {
              const reqRoomId = r.roomId || (r.room && (r.room.id || r.room))
              return reqRoomId === roomId && r.status === "pending"
            })
          : null
        setHasPendingRequest(!!userRequest)
        if (userRequest) {
          setPendingRequestId(userRequest.id)
        } else {
          setPendingRequestId(null)
        }
      } catch (err) {
        console.error("Ошибка перезагрузки заявок:", err)
      }
    } catch (err) {
      const errorMsg = err.message || "Не удалось отозвать заявку"
      setError(errorMsg)
      setAlertData({ title: "Ошибка", message: errorMsg, variant: "error" })
      setShowAlert(true)
    } finally {
      setLoading(false)
    }
  }

  // Показываем кнопку только для открытых комнат или комнат по заявкам
  // Если тип комнаты неизвестен, считаем её открытой (по умолчанию)
  if (!isOpenRoom && !isRequestRoom && roomInfo.joinType) {
    console.log("[PostRoomJoinButton] Room type not supported, hiding button")
    return null
  }
  
  console.log("[PostRoomJoinButton] Rendering button")

  return (
    <div style={{ 
      marginTop: "var(--spacing-sm)", 
      marginBottom: "var(--spacing-sm)",
      display: "flex",
      alignItems: "center",
      gap: "var(--spacing-sm)",
      flexWrap: "wrap"
    }}>
      {/* Кнопка метки комнаты - овальная, желтая outline */}
      <button
        onClick={handleRoomLabelClick}
        style={{
          display: "flex",
          alignItems: "center",
          gap: "var(--spacing-xs)",
          padding: "var(--spacing-xs) var(--spacing-md)",
          borderRadius: "20px", // Овальная форма
          border: "2px solid var(--accent-gold)",
          backgroundColor: "transparent",
          color: "var(--accent-gold)",
          fontSize: "var(--font-size-sm)",
          fontWeight: "500",
          cursor: "pointer",
          transition: "all 0.2s ease",
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.backgroundColor = "var(--accent-gold)"
          e.currentTarget.style.color = "var(--bg-primary)"
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.backgroundColor = "transparent"
          e.currentTarget.style.color = "var(--accent-gold)"
        }}
      >
        {/* Иконка двери (вход в комнату) */}
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
          <polyline points="10 17 15 12 10 7" />
          <line x1="15" y1="12" x2="3" y2="12" />
        </svg>
        <span>{roomName}</span>
      </button>

      {/* Кнопки присоединения к комнате */}
      <div style={{ display: "flex", alignItems: "center", gap: "var(--spacing-sm)" }}>
        {isOpenRoom ? (
          // Для открытых комнат - показываем только кнопку присоединения в стиле outline с иконкой +
          <button
            onClick={(e) => {
              e.stopPropagation()
              handleJoinOpenRoom()
            }}
            disabled={loading}
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: "var(--spacing-xs)",
              padding: "var(--spacing-xs) var(--spacing-md)",
              borderRadius: "var(--radius-md)",
              border: "2px solid var(--accent-gold)",
              backgroundColor: "transparent",
              color: "var(--accent-gold)",
              fontWeight: "500",
              fontSize: "var(--font-size-sm)",
              cursor: loading ? "not-allowed" : "pointer",
              opacity: loading ? 0.6 : 1,
              transition: "all 0.2s ease",
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                e.currentTarget.style.backgroundColor = "var(--accent-gold)"
                e.currentTarget.style.color = "var(--bg-primary)"
              }
            }}
            onMouseLeave={(e) => {
              if (!loading) {
                e.currentTarget.style.backgroundColor = "transparent"
                e.currentTarget.style.color = "var(--accent-gold)"
              }
            }}
          >
            {/* Иконка плюс */}
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>{loading ? "Присоединение..." : "Присоединиться"}</span>
          </button>
        ) : (
          // Для комнат по заявкам
          hasPendingRequest ? (
            <button
              className="btn btn-secondary"
              onClick={(e) => {
                e.stopPropagation()
                handleCancelRequest()
              }}
              disabled={loading}
              style={{
                backgroundColor: "var(--bg-tertiary)",
                color: "var(--text-primary)",
                fontWeight: "600",
                padding: "var(--spacing-xs) var(--spacing-md)",
                borderRadius: "var(--radius-md)",
                border: "1px solid var(--border-color)",
                cursor: loading ? "not-allowed" : "pointer",
                opacity: loading ? 0.7 : 1,
              }}
            >
              {loading ? "Отзыв..." : "Отозвать заявку"}
            </button>
          ) : (
            <button
              className="btn btn-primary"
              onClick={(e) => {
                e.stopPropagation()
                handleRequestJoin()
              }}
              disabled={loading}
              style={{
                backgroundColor: "var(--accent-gold)",
                color: "var(--bg-primary)",
                fontWeight: "600",
                padding: "var(--spacing-xs) var(--spacing-md)",
                borderRadius: "var(--radius-md)",
                border: "none",
                cursor: loading ? "not-allowed" : "pointer",
                opacity: loading ? 0.7 : 1,
              }}
            >
              {loading ? "Отправка..." : "Запросить вступление"}
            </button>
          )
        )}
      </div>
      {error && (
        <div style={{ 
          marginTop: "var(--spacing-xs)", 
          fontSize: "var(--font-size-sm)", 
          color: "var(--error-color)" 
        }}>
          {error}
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

      {/* Confirm Dialog for leaving room */}
      <ConfirmDialog
        open={showLeaveConfirm}
        title="Покинуть комнату"
        message="Вы уверены, что хотите покинуть эту комнату?"
        confirmText="Покинуть"
        cancelText="Отмена"
        confirmVariant="destructive"
        onConfirm={confirmLeaveOpenRoom}
        onCancel={() => setShowLeaveConfirm(false)}
      />

      {/* Confirm Dialog for canceling request */}
      <ConfirmDialog
        open={showCancelConfirm}
        title="Отозвать заявку"
        message="Вы уверены, что хотите отозвать заявку на вступление?"
        confirmText="Отозвать"
        cancelText="Отмена"
        confirmVariant="destructive"
        onConfirm={confirmCancelRequest}
        onCancel={() => setShowCancelConfirm(false)}
      />
    </div>
  )
}

