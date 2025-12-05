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

function RoomInfo({ onNavigate, roomId, currentPage }) {
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
    
    if (!confirm("Вы уверены, что хотите закрыть эту комнату? Все pending заявки будут автоматически отклонены.")) {
      return
    }

    try {
      await roomAPI.closeRoom(roomId, { userId: currentUser.id })
      // Reload room data
      const data = await roomAPI.getDetails(roomId)
      setRoomData(data)
      alert("Комната успешно закрыта")
    } catch (err) {
      const errorMessage = handleApiError(err)
      alert("Не удалось закрыть комнату: " + errorMessage)
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
                              backgroundColor: "transparent",
                              border: "none",
                              width: "40px",
                              height: "40px",
                              flexShrink: 0
                            }}
                          />
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
                {isMember ? (
                  <button 
                    className="btn btn-primary" 
                    style={{ flex: 1 }}
                    onClick={() => onNavigate("chat", roomId)}
                  >
                    Открыть чат
                  </button>
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
                    onError={(error) => {
                      alert(error)
                    }}
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
                          color: "var(--destructive)",
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
                      onError={(error) => {
                        alert(error)
                      }}
                    />
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
