"use client"
import { useState, useEffect } from "react"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"
import "../styles/navigation.css"
import { roomAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import BottomNavigation from "./BottomNavigation"
import { JoinRequestButton, PendingRequestsList } from "../components/rooms"
import { handleApiError } from "../types"
import { ConfirmDialog } from "../components/ui/ConfirmDialog"
import { AlertDialog } from "../components/ui/AlertDialog"
import { getAvatarEmoji } from "../utils/avatarUtils"

function RoomInfo({ onNavigate, roomId, currentPage, onRoomUpdated }) {
  const { currentUser } = useUser()
  const [roomData, setRoomData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [isMember, setIsMember] = useState(false)
  const [hasPendingRequest, setHasPendingRequest] = useState(false)
  const [pendingRequestId, setPendingRequestId] = useState(null)
  const [pendingRequest, setPendingRequest] = useState(null)
  const [isAdmin, setIsAdmin] = useState(false)
  const [activeTab, setActiveTab] = useState("info") // "info" or "posts"
  const [showCloseConfirm, setShowCloseConfirm] = useState(false)
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)
  const [showAlert, setShowAlert] = useState(false)
  const [alertData, setAlertData] = useState({ title: "", message: "", variant: "info" })
  const [isLeaving, setIsLeaving] = useState(false)
  const [showPromoteConfirm, setShowPromoteConfirm] = useState(false)
  const [showKickConfirm, setShowKickConfirm] = useState(false)
  const [selectedMember, setSelectedMember] = useState(null)

  useEffect(() => {
    const loadRoomData = async () => {
      if (!roomId) {
        setLoading(false)
        return
      }

      setLoading(true)
      setError("")
      try {
        const data = await roomAPI.getDetails(roomId)
        console.log("[RoomInfo] Received room data:", data)
        console.log("[RoomInfo] Members:", data.members)
        setRoomData(data)
        
        // Check if user is a member and admin
        if (currentUser?.id) {
          // Check if current user is admin
          const currentUserMember = data.members?.find(m => m.id === currentUser.id)
          setIsAdmin(currentUserMember?.isAdmin || false)
          try {
            const userRooms = await roomAPI.getUserRooms(currentUser.id)
            const isInRoom = Array.isArray(userRooms) && userRooms.some(r => r.id === roomId)
            setIsMember(isInRoom)
            
            // Check if user is admin/creator
            const isUserAdmin = data.creatorId === currentUser.id || 
              (data.members && Array.isArray(data.members) && 
               data.members.some(m => m.id === currentUser.id && m.isAdmin))
            setIsAdmin(isUserAdmin)
            
            // Check for pending request
            if (!isInRoom && data.joinType === "by_application") {
              try {
                const myRequests = await roomAPI.getMyPendingRequests(currentUser.id)
                const pending = Array.isArray(myRequests) && myRequests.find(r => 
                  r.roomId === roomId && r.status === "pending"
                )
                if (pending) {
                  setHasPendingRequest(true)
                  setPendingRequestId(pending.id)
                  setPendingRequest(pending)
                } else {
                  // Check for rejected request (for cooldown display)
                  const rejected = Array.isArray(myRequests) && myRequests.find(r => 
                    r.roomId === roomId && r.status === "rejected"
                  )
                  if (rejected) {
                    setPendingRequest(rejected)
                  }
                }
              } catch (err) {
                console.error("Error checking pending requests:", err)
              }
            }
          } catch (err) {
            console.error("Error checking membership:", err)
          }
        }
      } catch (err) {
        console.error("Ошибка загрузки информации о комнате:", err)
        setError(err.message || "Не удалось загрузить информацию о комнате")
      } finally {
        setLoading(false)
      }
    }

    loadRoomData()
  }, [roomId, currentUser])

  const handleRequestCreated = async (request) => {
    // For open rooms, request might be empty object (direct join)
    if (!request || !request.id) {
      // Direct join to open room - reload room data and set as member
      try {
        const data = await roomAPI.getDetails(roomId)
        setRoomData(data)
        const members = data.members || []
        const isInRoom = members.some(m => (m.id || m.userId) === currentUser.id)
        setIsMember(isInRoom)
      } catch (err) {
        console.error("Error reloading room data after join:", err)
        // Still set as member if join was successful
        setIsMember(true)
      }
      return
    }
    
    // For application-based rooms
    setHasPendingRequest(true)
    setPendingRequestId(request.id)
    setPendingRequest(request)
    // Reload room data to update member count if approved
    if (request.status === "approved") {
      const data = await roomAPI.getDetails(roomId)
      setRoomData(data)
      setIsMember(true)
    }
  }

  const handleRequestCancelled = async () => {
    setHasPendingRequest(false)
    setPendingRequestId(null)
    setPendingRequest(null)
  }

  const handleRequestApproved = async () => {
    // Reload room data to update member count
    const data = await roomAPI.getDetails(roomId)
    setRoomData(data)
  }

  const handleRequestRejected = async () => {
    // Refresh pending requests list
    // The PendingRequestsList component will handle its own refresh
  }

  const handleCloseRoom = async () => {
    if (!currentUser?.id || !roomId) return
    setShowCloseConfirm(true)
  }

  const confirmCloseRoom = async () => {
    setShowCloseConfirm(false)
    
    try {
      await roomAPI.closeRoom(roomId, { userId: currentUser.id })
      // Reload room data
      const data = await roomAPI.getDetails(roomId)
      setRoomData(data)
      
      // Dispatch custom event to notify RoomsList to refresh
      window.dispatchEvent(new CustomEvent('roomUpdated', { detail: { roomId } }))
      
      // Also call callback if provided
      if (onRoomUpdated) {
        onRoomUpdated()
      }
      
      setAlertData({
        title: "Успешно",
        message: "Комната успешно закрыта",
        variant: "success"
      })
      setShowAlert(true)
    } catch (err) {
      const errorMessage = handleApiError(err)
      setAlertData({
        title: "Ошибка",
        message: "Не удалось закрыть комнату: " + errorMessage,
        variant: "error"
      })
      setShowAlert(true)
    }
  }

  const showErrorAlert = (message) => {
    setAlertData({
      title: "Ошибка",
      message: message,
      variant: "error"
    })
    setShowAlert(true)
  }

  const handleLeaveRoom = () => {
    if (!currentUser?.id || !roomId) return
    setShowLeaveConfirm(true)
  }

  const confirmLeaveRoom = async () => {
    setShowLeaveConfirm(false)
    setIsLeaving(true)
    
    try {
      await roomAPI.removeUserFromRoom(roomId, currentUser.id)
      
      // Reload room data
      const data = await roomAPI.getDetails(roomId)
      setRoomData(data)
      setIsMember(false)
      
      // Dispatch custom event to notify RoomsList to refresh
      window.dispatchEvent(new CustomEvent('roomUpdated', { detail: { roomId } }))
      
      // Also call callback if provided
      if (onRoomUpdated) {
        onRoomUpdated()
      }
      
      setAlertData({
        title: "Успешно",
        message: "Вы покинули комнату",
        variant: "success"
      })
      setShowAlert(true)
    } catch (err) {
      const errorMessage = handleApiError(err)
      setAlertData({
        title: "Ошибка",
        message: "Не удалось покинуть комнату: " + errorMessage,
        variant: "error"
      })
      setShowAlert(true)
    } finally {
      setIsLeaving(false)
    }
  }

  const confirmPromoteToAdmin = async () => {
    if (!selectedMember || !currentUser?.id || !roomId) return

    setShowPromoteConfirm(false)
    
    try {
      await roomAPI.promoteToAdmin(roomId, selectedMember.id, currentUser.id)
      
      // Reload room data to update member list
      const data = await roomAPI.getDetails(roomId)
      setRoomData(data)
      
      // Update admin status if current user was involved
      if (currentUser.id === selectedMember.id) {
        setIsAdmin(true)
      }
      
      setAlertData({
        title: "Успешно",
        message: `${selectedMember.name || selectedMember.username} назначен администратором`,
        variant: "success"
      })
      setShowAlert(true)
      setSelectedMember(null)
    } catch (err) {
      const errorMessage = handleApiError(err)
      setAlertData({
        title: "Ошибка",
        message: "Не удалось назначить администратором: " + errorMessage,
        variant: "error"
      })
      setShowAlert(true)
    }
  }

  const confirmKickUser = async () => {
    if (!selectedMember || !currentUser?.id || !roomId) return

    setShowKickConfirm(false)
    
    try {
      await roomAPI.kickUserFromRoom(roomId, selectedMember.id, currentUser.id)
      
      // Reload room data to update member list
      const data = await roomAPI.getDetails(roomId)
      setRoomData(data)
      
      setAlertData({
        title: "Успешно",
        message: `${selectedMember.name || selectedMember.username} исключен из комнаты`,
        variant: "success"
      })
      setShowAlert(true)
      setSelectedMember(null)
    } catch (err) {
      const errorMessage = handleApiError(err)
      setAlertData({
        title: "Ошибка",
        message: "Не удалось исключить пользователя: " + errorMessage,
        variant: "error"
      })
      setShowAlert(true)
    }
  }

  const handleRequestRating = async () => {
    if (!currentUser?.id || !roomId || !roomData?.members || !roomData?.creatorId) {
      return
    }

    // Filter out creator and current user from members list
    const membersToNotify = roomData.members.filter(member => 
      member.id !== roomData.creatorId && member.id !== currentUser.id
    )

    if (membersToNotify.length === 0) {
      setAlertData({
        title: "Информация",
        message: "Нет участников для запроса оценки",
        variant: "info"
      })
      setShowAlert(true)
      return
    }

    try {
      // Request rating from all participants for the creator
      // requestedUserId = creatorId (who should be rated)
      // requesterUserId = currentUser.id (who is requesting)
      await roomAPI.createRatingRequest(roomId, roomData.creatorId, currentUser.id)
      
      setAlertData({
        title: "Успешно",
        message: `Запрос на оценку отправлен ${membersToNotify.length} участникам`,
        variant: "success"
      })
      setShowAlert(true)
    } catch (err) {
      const errorMessage = handleApiError(err)
      setAlertData({
        title: "Ошибка",
        message: "Не удалось отправить запрос на оценку: " + errorMessage,
        variant: "error"
      })
      setShowAlert(true)
    }
  }

  if (loading) {
    return (
      <div>
        <div className="top-nav">
          <button className="btn-icon" onClick={() => onNavigate("rooms")}>
            ←
          </button>
          <div className="top-nav-title">Загрузка...</div>
          <div style={{ width: "40px" }}></div>
        </div>
        <div style={{ padding: "var(--spacing-lg)", textAlign: "center" }}>Загрузка информации о комнате...</div>
      </div>
    )
  }

  if (error || !roomData) {
    return (
      <div>
        <div className="top-nav">
          <button className="btn-icon" onClick={() => onNavigate("rooms")}>
            ←
          </button>
          <div className="top-nav-title">Ошибка</div>
          <div style={{ width: "40px" }}></div>
        </div>
        <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--error)" }}>
          {error || "Комната не найдена"}
        </div>
      </div>
    )
  }

  const progressPercent = roomData.maxCollaborators 
    ? (roomData.memberCount / roomData.maxCollaborators) * 100 
    : 0

  return (
    <div>
      <div className="top-nav">
        <button className="btn-icon" onClick={() => onNavigate("rooms")}>
          ←
        </button>
        <div className="top-nav-title">Информация о комнате</div>
        <div style={{ width: "40px" }}></div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        <button 
          className={`tab ${activeTab === "info" ? "active" : ""}`} 
          onClick={() => setActiveTab("info")}
        >
          Информация
        </button>
        <button 
          className={`tab ${activeTab === "posts" ? "active" : ""}`} 
          onClick={() => setActiveTab("posts")}
        >
          Посты ({roomData.pinnedPostCount || 0})
        </button>
      </div>

      {activeTab === "info" ? (
        <div className="room-info-container">
          <div className="room-info-card">
            {/* Header */}
            <div className="room-info-header">
              <h1 className="room-info-title">{roomData.name || "Без названия"}</h1>
              {roomData.category && (
                <span className="room-info-category">{roomData.category}</span>
              )}
              {roomData.isDefault && (
                <span className="badge" style={{ marginLeft: "var(--spacing-sm)" }}>По умолчанию</span>
              )}
            </div>

            {/* Description */}
            <div className="room-info-section">
              <div className="room-info-label">Описание</div>
              <p className="room-info-description">
                {roomData.description || "Описание не предоставлено"}
              </p>
            </div>

            {/* Location */}
            {roomData.location && (
              <div className="room-info-section">
                <div className="room-info-label">Местоположение</div>
                <div className="room-info-value">📍 {roomData.location}</div>
              </div>
            )}

            {/* Meeting time */}
            {roomData.meetingTime && (
              <div className="room-info-section">
                <div className="room-info-label">Дата и время проведения</div>
                <div className="room-info-value">
                  {new Date(roomData.meetingTime).toLocaleString("ru-RU")}
                </div>
              </div>
            )}

            {/* End time */}
            {roomData.endTime && (
              <div className="room-info-section">
                <div className="room-info-label">Дата окончания существования</div>
                <div className="room-info-value">
                  {new Date(roomData.endTime).toLocaleString("ru-RU")}
                </div>
                <div style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)", marginTop: "var(--spacing-xs)" }}>
                  Комната будет удалена через сутки после этой даты
                </div>
              </div>
            )}

            {/* Meeting type */}
            {roomData.meetingType && (
              <div className="room-info-section">
                <div className="room-info-label">Формат</div>
                <div className="room-info-value">
                  {roomData.meetingType === "online" ? "Онлайн" : "Очно"}
                </div>
              </div>
            )}

            {/* Join type */}
            <div className="room-info-section">
              <div className="room-info-label">Тип набора</div>
              <div className="room-info-value">
                {roomData.joinType === "by_application" || roomData.joinType === "REQUEST_ONLY" ? "По заявкам" : "Открытая"}
              </div>
            </div>

            {/* Members */}
            <div className="room-info-section">
              <div className="room-info-label">Участники</div>
              <div className="room-info-value">
                {roomData.memberCount} {roomData.maxCollaborators ? `из ${roomData.maxCollaborators}` : ""}
              </div>
              {roomData.maxCollaborators && (
                <div className="room-members-progress">
                  <div className="progress-bar">
                    <div className="progress-fill" style={{ width: `${Math.min(progressPercent, 100)}%` }}></div>
                  </div>
                  <div className="progress-text">
                    {roomData.maxCollaborators - roomData.memberCount} мест осталось
                  </div>
                </div>
              )}
              
              {/* Members List */}
              {roomData.members && Array.isArray(roomData.members) && roomData.members.length > 0 ? (
                <div style={{ 
                  marginTop: "var(--spacing-lg)",
                  paddingTop: "var(--spacing-lg)",
                  borderTop: "1px solid var(--border-color)"
                }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "var(--spacing-sm)",
                    marginBottom: "var(--spacing-md)",
                    paddingBottom: "var(--spacing-sm)",
                    borderBottom: "1px solid var(--border-color)"
                  }}>
                    <svg 
                      width="18" 
                      height="18" 
                      viewBox="0 0 24 24" 
                      fill="none" 
                      stroke="var(--accent-gold)" 
                      strokeWidth="2"
                      style={{ opacity: 0.8 }}
                    >
                      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                      <circle cx="9" cy="7" r="4" />
                      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                    </svg>
                    <span style={{
                      fontSize: "var(--font-size-sm)",
                      color: "var(--accent-gold)",
                      fontWeight: "600",
                      textTransform: "uppercase",
                      letterSpacing: "0.5px"
                    }}>
                      Список участников
                    </span>
                  </div>
                  <div style={{ 
                    display: "flex", 
                    flexDirection: "column", 
                    gap: "var(--spacing-xs)",
                    maxHeight: "300px",
                    overflowY: "auto",
                    padding: "var(--spacing-xs)",
                    borderRadius: "var(--radius-md)",
                    backgroundColor: "var(--bg-tertiary)"
                  }}>
                    {roomData.members.map((member) => (
                      <div
                        key={member.id}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "var(--spacing-md)",
                          padding: "var(--spacing-sm) var(--spacing-md)",
                          borderRadius: "var(--radius-md)",
                          cursor: "pointer",
                          transition: "all 0.2s ease",
                          backgroundColor: "transparent"
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.backgroundColor = "var(--bg-secondary)"
                          e.currentTarget.style.transform = "translateX(2px)"
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.backgroundColor = "transparent"
                          e.currentTarget.style.transform = "translateX(0)"
                        }}
                        onClick={() => {
                          if (member.id) {
                            onNavigate("profile", member.id)
                          }
                        }}
                      >
                        {member.avatar?.id ? (
                          <img
                            src={imageAPI.getImageUrl(member.avatar.id)}
                            alt={member.name || member.username}
                            className="avatar avatar-md"
                            style={{ flexShrink: 0 }}
                          />
                        ) : (
                          <div
                            className="avatar avatar-md"
                            style={{
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              backgroundColor: "var(--bg-tertiary)",
                              border: "1px solid var(--border-primary)",
                              borderRadius: "50%",
                              fontSize: "var(--font-size-base)",
                              width: "40px",
                              height: "40px",
                              flexShrink: 0
                            }}
                          >
                            {getAvatarEmoji(member.id)}
                          </div>
                        )}
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ 
                            fontSize: "var(--font-size-base)", 
                            fontWeight: "600", 
                            color: "var(--text-primary)",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                            display: "flex",
                            alignItems: "center",
                            gap: "var(--spacing-xs)"
                          }}>
                            <span>{member.name || member.username}</span>
                            {member.isAdmin && (
                              <span style={{
                                fontSize: "var(--font-size-xs)",
                                color: "var(--accent-gold)",
                                opacity: 0.8,
                                fontWeight: "500"
                              }}>
                                (админ)
                              </span>
                            )}
                          </div>
                          {member.rating != null && member.rating > 0 && (
                            <div style={{ 
                              fontSize: "var(--font-size-sm)", 
                              color: "var(--text-muted)",
                              display: "flex",
                              alignItems: "center",
                              gap: "var(--spacing-xs)"
                            }}>
                              <span className="badge badge-rating">{member.rating.toFixed(1)}</span>
                            </div>
                          )}
                        </div>
                        {isAdmin && currentUser?.id && member.id !== currentUser.id && roomData.creatorId !== member.id && (
                          <div style={{ display: "flex", gap: "var(--spacing-xs)", flexShrink: 0 }}>
                            {!member.isAdmin && (
                              <button
                                className="btn btn-secondary"
                                style={{ 
                                  fontSize: "var(--font-size-xs)",
                                  padding: "var(--spacing-xs) var(--spacing-sm)"
                                }}
                                onClick={(e) => {
                                  e.stopPropagation()
                                  setSelectedMember(member)
                                  setShowPromoteConfirm(true)
                                }}
                              >
                                Сделать админом
                              </button>
                            )}
                            <button
                              className="btn btn-secondary"
                              style={{ 
                                fontSize: "var(--font-size-xs)",
                                padding: "var(--spacing-xs) var(--spacing-sm)",
                                backgroundColor: "var(--error)",
                                color: "white"
                              }}
                              onClick={(e) => {
                                e.stopPropagation()
                                setSelectedMember(member)
                                setShowKickConfirm(true)
                              }}
                            >
                              Выгнать
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              ) : roomData.members !== undefined && roomData.members !== null && roomData.members.length === 0 ? (
                <div style={{ 
                  marginTop: "var(--spacing-lg)",
                  paddingTop: "var(--spacing-lg)",
                  borderTop: "1px solid var(--border-color)",
                  textAlign: "center",
                  padding: "var(--spacing-md)",
                  color: "var(--text-muted)",
                  fontSize: "var(--font-size-sm)",
                  fontStyle: "italic"
                }}>
                  В комнате пока нет участников
                </div>
              ) : null}
            </div>

            {/* Creator */}
            {roomData.creatorName && (
              <div className="room-info-section">
                <div className="room-info-label">Создатель</div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "var(--spacing-md)",
                    marginTop: "var(--spacing-sm)",
                  }}
                >
                  <div>
                    <div style={{ fontSize: "var(--font-size-base)", fontWeight: "600", color: "var(--text-primary)" }}>
                      {roomData.creatorName}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Created date */}
            {roomData.createdAt && (
              <div
                className="room-info-section"
                style={{ paddingTop: "var(--spacing-lg)", borderTop: "1px solid var(--border-color)" }}
              >
                <div className="room-info-label">Комната создана</div>
                <div style={{ color: "var(--text-muted)", fontSize: "var(--font-size-sm)" }}>
                  {new Date(roomData.createdAt).toLocaleDateString("ru-RU")}
                </div>
              </div>
            )}

            {/* Actions */}
            {currentUser && (
              <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-md)", marginTop: "var(--spacing-lg)" }}>
                {roomData.isClosed ? (
                  <div style={{
                    padding: "var(--spacing-md)",
                    backgroundColor: "var(--bg-secondary)",
                    borderRadius: "var(--radius-md)",
                    border: "1px solid var(--error-color)",
                    textAlign: "center"
                  }}>
                    <div style={{ 
                      color: "var(--error-color)", 
                      fontWeight: "600",
                      marginBottom: "var(--spacing-xs)"
                    }}>
                      Комната закрыта
                    </div>
                    <div style={{ 
                      fontSize: "var(--font-size-sm)", 
                      color: "var(--text-muted)" 
                    }}>
                      {isAdmin || isMember 
                        ? "Вы можете просматривать информацию о комнате, но новые участники не принимаются."
                        : "Эта комната больше не принимает новых участников."}
                    </div>
                  </div>
                ) : isMember ? (
                  <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-sm)" }}>
                    <button 
                      className="btn btn-primary" 
                      style={{ width: "100%" }}
                      onClick={() => onNavigate("chat", roomId)}
                    >
                      Открыть чат
                    </button>
                    
                    {/* Кнопка выхода из комнаты в стиле Material Design */}
                    <button
                      onClick={handleLeaveRoom}
                      disabled={isLeaving || roomData.creatorId === currentUser.id}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        gap: "var(--spacing-xs)",
                        padding: "var(--spacing-sm) var(--spacing-md)",
                        borderRadius: "var(--radius-md)",
                        border: "1px solid var(--border-color)",
                        backgroundColor: "transparent",
                        color: roomData.creatorId === currentUser.id ? "var(--text-muted)" : "var(--text-secondary)",
                        fontSize: "var(--font-size-sm)",
                        fontWeight: "500",
                        cursor: roomData.creatorId === currentUser.id ? "not-allowed" : (isLeaving ? "wait" : "pointer"),
                        transition: "all 0.2s ease",
                        opacity: isLeaving ? 0.6 : 1,
                      }}
                      onMouseEnter={(e) => {
                        if (roomData.creatorId !== currentUser.id && !isLeaving) {
                          e.currentTarget.style.backgroundColor = "var(--bg-secondary)"
                          e.currentTarget.style.borderColor = "var(--text-muted)"
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (roomData.creatorId !== currentUser.id) {
                          e.currentTarget.style.backgroundColor = "transparent"
                          e.currentTarget.style.borderColor = "var(--border-color)"
                        }
                      }}
                      title={roomData.creatorId === currentUser.id ? "Создатель комнаты не может покинуть её" : "Покинуть комнату"}
                    >
                      {/* Иконка выхода */}
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
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                        <polyline points="16 17 21 12 16 7" />
                        <line x1="21" y1="12" x2="9" y2="12" />
                      </svg>
                      <span>{isLeaving ? "Выход..." : "Покинуть комнату"}</span>
                    </button>
                    
                    {roomData.creatorId === currentUser.id && (
                      <div style={{ 
                        fontSize: "var(--font-size-xs)", 
                        color: "var(--text-muted)", 
                        textAlign: "center",
                        fontStyle: "italic"
                      }}>
                        Создатель комнаты не может покинуть её
                      </div>
                    )}
                  </div>
                ) : (
                  <JoinRequestButton
                    roomId={roomId}
                    roomJoinType={
                      roomData.joinType === "by_application" || roomData.joinType === "REQUEST_ONLY"
                        ? "by_application"
                        : "open"
                    }
                    isMember={isMember}
                    hasPendingRequest={hasPendingRequest}
                    pendingRequestId={pendingRequestId}
                    pendingRequest={pendingRequest}
                    userId={currentUser.id}
                    onRequestCreated={handleRequestCreated}
                    onRequestCancelled={handleRequestCancelled}
                    onError={showErrorAlert}
                    onNavigate={onNavigate}
                  />
                )}

                {/* Admin Section */}
                {isAdmin && (
                  <div style={{ 
                    marginTop: "var(--spacing-lg)",
                    paddingTop: "var(--spacing-lg)",
                    borderTop: "1px solid var(--border-color)"
                  }}>
                    <div style={{ 
                      display: "flex", 
                      justifyContent: "space-between", 
                      alignItems: "center",
                      marginBottom: "var(--spacing-md)"
                    }}>
                      <h3 style={{ margin: 0, fontSize: "var(--font-size-lg)" }}>Управление комнатой</h3>
                      {!roomData.isClosed && (
                        <button
                          className="btn btn-outline"
                          onClick={handleCloseRoom}
                          style={{ fontSize: "var(--font-size-sm)" }}
                        >
                          Закрыть комнату
                        </button>
                      )}
                      {roomData.isClosed && (
                        <span style={{ 
                          fontSize: "var(--font-size-sm)",
                          color: "var(--error-color)",
                          fontWeight: "600"
                        }}>
                          Комната закрыта
                        </span>
                      )}
                    </div>
                    <PendingRequestsList
                      roomId={roomId}
                      userId={currentUser.id}
                      isAdmin={isAdmin}
                      isRoomClosed={roomData.isClosed || false}
                      onRequestApproved={handleRequestApproved}
                      onRequestRejected={handleRequestRejected}
                      onError={showErrorAlert}
                    />
                    
                    {/* Request Rating Button */}
                    {roomData.members && roomData.members.length > 0 && (
                      <div style={{ marginTop: "var(--spacing-md)" }}>
                        <button
                          className="btn btn-primary"
                          onClick={handleRequestRating}
                          style={{ 
                            width: "100%",
                            fontSize: "var(--font-size-sm)"
                          }}
                        >
                          Запросить оценку у участников
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      ) : (
        <RoomPostsTab roomId={roomId} onNavigate={onNavigate} currentUser={currentUser} />
      )}

      <BottomNavigation currentPage={currentPage || "rooms"} onNavigate={onNavigate} />

      {/* Confirmation Dialog for closing room */}
      <ConfirmDialog
        open={showCloseConfirm}
        title="Закрыть комнату"
        message="Вы уверены, что хотите закрыть эту комнату? Все pending заявки будут автоматически отклонены."
        confirmText="Закрыть"
        cancelText="Отмена"
        confirmVariant="destructive"
        onConfirm={confirmCloseRoom}
        onCancel={() => setShowCloseConfirm(false)}
      />

      {/* Confirmation Dialog for leaving room */}
      <ConfirmDialog
        open={showLeaveConfirm}
        title="Покинуть комнату"
        message="Вы уверены, что хотите покинуть эту комнату? Вы потеряете доступ к чату и всем материалам комнаты."
        confirmText="Покинуть"
        cancelText="Отмена"
        confirmVariant="destructive"
        onConfirm={confirmLeaveRoom}
        onCancel={() => setShowLeaveConfirm(false)}
      />

      {/* Alert Dialog */}
      <AlertDialog
        open={showAlert}
        title={alertData.title}
        message={alertData.message}
        variant={alertData.variant}
        onClose={() => setShowAlert(false)}
      />

      {/* Confirm Dialog for promoting to admin */}
      <ConfirmDialog
        open={showPromoteConfirm}
        title="Назначить администратором"
        message={selectedMember ? `Назначить ${selectedMember.name || selectedMember.username} администратором?` : ""}
        confirmText="Назначить"
        cancelText="Отмена"
        onConfirm={confirmPromoteToAdmin}
        onCancel={() => {
          setShowPromoteConfirm(false)
          setSelectedMember(null)
        }}
      />

      {/* Confirm Dialog for kicking user */}
      <ConfirmDialog
        open={showKickConfirm}
        title="Исключить из комнаты"
        message={selectedMember ? `Выгнать ${selectedMember.name || selectedMember.username} из комнаты?` : ""}
        confirmText="Выгнать"
        cancelText="Отмена"
        confirmVariant="destructive"
        onConfirm={confirmKickUser}
        onCancel={() => {
          setShowKickConfirm(false)
          setSelectedMember(null)
        }}
      />
    </div>
  )
}

function RoomPostsTab({ roomId, onNavigate, currentUser }) {
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    const loadPosts = async () => {
      if (!roomId) {
        setLoading(false)
        return
      }

      setLoading(true)
      setError("")
      try {
        const data = await roomAPI.getPinnedPosts(roomId)
        setPosts(Array.isArray(data) ? data : [])
      } catch (err) {
        console.error("Ошибка загрузки постов:", err)
        setError(err.message || "Не удалось загрузить посты")
        setPosts([])
      } finally {
        setLoading(false)
      }
    }

    loadPosts()
  }, [roomId])

  if (loading) {
    return (
      <div style={{ padding: "var(--spacing-lg)", textAlign: "center" }}>Загрузка постов...</div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--error)" }}>
        {error}
      </div>
    )
  }

  if (posts.length === 0) {
    return (
      <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--text-muted)" }}>
        Нет закрепленных постов в этой комнате
      </div>
    )
  }

  return (
    <div style={{ padding: "var(--spacing-md)", paddingBottom: "80px" }}>
      {posts.map((post) => (
        <div
          key={post.id}
          className="post-card"
          onClick={() => {
            const postId = typeof post.id === "object" ? (post.id?.id || post.id?.postId || null) : post.id
            if (postId) {
              onNavigate("comments", postId)
            }
          }}
        >
          <div className="post-header">
            {post.author?.avatar?.id ? (
              <img
                src={imageAPI.getImageUrl(post.author.avatar.id)}
                alt={post.author?.name || "Пользователь"}
                className="avatar avatar-md avatar-clickable"
                onClick={(e) => {
                  e.stopPropagation()
                  const userId = post.author?.id
                  if (userId) {
                    onNavigate("profile", userId)
                  }
                }}
              />
              ) : (
                <div
                  className="avatar avatar-md avatar-clickable"
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    backgroundColor: "var(--bg-tertiary)",
                    border: "1px solid var(--border-primary)",
                    borderRadius: "50%",
                    fontSize: "var(--font-size-base)",
                    width: "40px",
                    height: "40px",
                    cursor: "pointer"
                  }}
                  onClick={(e) => {
                    e.stopPropagation()
                    const userId = post.author?.id
                    if (userId) {
                      onNavigate("profile", userId)
                    }
                  }}
                >
                  {getAvatarEmoji(post.author?.id)}
                </div>
              )}
            <div className="post-user-info">
              <div className="post-username">
                {post.author?.name || post.author?.username || "Пользователь"}
              </div>
              <div className="post-time">
                {post.createdAt ? new Date(post.createdAt).toLocaleDateString("ru-RU") : ""}
              </div>
            </div>
          </div>

          <h3 className="post-title">{post.name || post.title}</h3>
          <p className="post-content">{post.text || post.content}</p>

          {post.image && (
            <img
              src={
                typeof post.image === "object" && post.image.id
                  ? imageAPI.getImageUrl(post.image.id)
                  : post.image?.url || post.image || "/placeholder.svg"
              }
              alt={post.name || post.title}
              className="post-image"
              style={{ marginTop: "var(--spacing-sm)", marginBottom: "var(--spacing-sm)" }}
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

          {/* Show room label if post is attached to a room (different from current room) */}
          {post.room && (typeof post.room === "object" ? post.room.id !== roomId : true) && (
            <div style={{ 
              marginTop: "var(--spacing-sm)", 
              marginBottom: "var(--spacing-sm)",
              display: "flex",
              alignItems: "center",
              gap: "var(--spacing-sm)"
            }}>
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  const attachedRoomId = typeof post.room === "object" 
                    ? (post.room?.id || post.roomId || null) 
                    : post.roomId
                  if (attachedRoomId) {
                    onNavigate("roomInfo", attachedRoomId)
                  }
                }}
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
                <span>
                  {typeof post.room === "object" && post.room?.name ? post.room.name : "Комната"}
                </span>
              </button>
            </div>
          )}

          {post.image && (
            <img
              src={post.image?.id ? `http://localhost:8080/images/${post.image.id}` : "/placeholder.svg"}
              alt={post.name || post.title}
              className="post-image"
            />
          )}
        </div>
      ))}
    </div>
  )
}

export default RoomInfo
