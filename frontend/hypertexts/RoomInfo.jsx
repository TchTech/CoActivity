"use client"
import { useState, useEffect } from "react"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"
import "../styles/navigation.css"
import { roomAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import BottomNavigation from "./BottomNavigation"

function RoomInfo({ onNavigate, roomId }) {
  const { currentUser } = useUser()
  const [roomData, setRoomData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [isMember, setIsMember] = useState(false)
  const [hasPendingRequest, setHasPendingRequest] = useState(false)
  const [pendingRequestId, setPendingRequestId] = useState(null)
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
        setRoomData(data)
        
        // Check if user is a member
        if (currentUser?.id) {
          try {
            const userRooms = await roomAPI.getUserRooms(currentUser.id)
            const isInRoom = Array.isArray(userRooms) && userRooms.some(r => r.id === roomId)
            setIsMember(isInRoom)
            
            // Check for pending request
            if (!isInRoom && data.joinType === "by_application") {
              try {
                const myRequests = await roomAPI.getMyPendingRequests(currentUser.id)
                const pending = Array.isArray(myRequests) && myRequests.find(r => 
                  r.room?.id === roomId && r.status === "pending"
                )
                if (pending) {
                  setHasPendingRequest(true)
                  setPendingRequestId(pending.id)
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

  const handleApply = async () => {
    if (!currentUser?.id || !roomId) return

    try {
      const request = await roomAPI.createMembershipRequest(roomId, currentUser.id, "")
      setHasPendingRequest(true)
      setPendingRequestId(request.id)
    } catch (err) {
      console.error("Ошибка подачи заявки:", err)
      alert("Не удалось подать заявку: " + (err.message || "Неизвестная ошибка"))
    }
  }

  const handleCancelRequest = async () => {
    if (!currentUser?.id || !roomId || !pendingRequestId) return

    try {
      await roomAPI.cancelMembershipRequest(roomId, pendingRequestId, currentUser.id)
      setHasPendingRequest(false)
      setPendingRequestId(null)
    } catch (err) {
      console.error("Ошибка отмены заявки:", err)
      alert("Не удалось отменить заявку: " + (err.message || "Неизвестная ошибка"))
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
                {roomData.joinType === "by_application" ? "По заявкам" : "Открытая"}
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
              <div style={{ display: "flex", gap: "var(--spacing-md)", marginTop: "var(--spacing-lg)" }}>
                {!isMember && roomData.joinType === "by_application" && (
                  hasPendingRequest ? (
                    <button className="btn btn-secondary" style={{ flex: 1 }} onClick={handleCancelRequest}>
                      Заявка отправлена (отменить)
                    </button>
                  ) : (
                    <button className="btn btn-primary" style={{ flex: 1 }} onClick={handleApply}>
                      Подать заявку
                    </button>
                  )
                )}
                {isMember && (
                  <button 
                    className="btn btn-primary" 
                    style={{ flex: 1 }}
                    onClick={() => onNavigate("chat", roomId)}
                  >
                    Открыть чат
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      ) : (
        <RoomPostsTab roomId={roomId} onNavigate={onNavigate} currentUser={currentUser} />
      )}

      <BottomNavigation currentPage="rooms" onNavigate={onNavigate} />
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
            <img
              src={post.author?.avatar?.id ? `http://localhost:8080/images/${post.author.avatar.id}` : "/placeholder.svg"}
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
