"use client"
import { useEffect, useState } from "react"
import BottomNavigation from "./BottomNavigation"
import { roomAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"
import "../styles/navigation.css"

function RoomsList({ onNavigate }) {
  const { currentUser } = useUser()
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    const loadRooms = async () => {
      if (!currentUser?.id) {
        setLoading(false)
        return
      }

      setLoading(true)
      setError("")
      try {
        const data = await roomAPI.getUserRooms(currentUser.id)
        // Sort rooms by creation date (newest first)
        const sortedRooms = Array.isArray(data)
          ? data.sort((a, b) => {
              const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
              const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
              return dateB - dateA
            })
          : []
        setRooms(sortedRooms)
      } catch (err) {
        console.error("Ошибка загрузки комнат:", err)
        setError(err.message || "Не удалось загрузить комнаты")
      } finally {
        setLoading(false)
      }
    }

    loadRooms()
  }, [currentUser])

  // Refresh rooms when navigating back to this page
  // This ensures newly created rooms appear immediately
  useEffect(() => {
    if (currentUser?.id) {
      const loadRooms = async () => {
        try {
          const data = await roomAPI.getUserRooms(currentUser.id)
          const sortedRooms = Array.isArray(data)
            ? data.sort((a, b) => {
                const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
                const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
                return dateB - dateA
              })
            : []
          setRooms(sortedRooms)
        } catch (err) {
          console.error("Ошибка обновления комнат:", err)
        }
      }
      loadRooms()
    }
  }, [])

  return (
    <div>
      {/* Верхняя навигация */}
      <div className="top-nav">
        <div className="top-nav-title">Мои комнаты</div>
        <div className="top-nav-actions">
          <button className="btn-icon">🔍</button>
          <button className="btn-icon" onClick={() => onNavigate("createRoom")}>
            +
          </button>
        </div>
      </div>

      <div className="rooms-list-container">
        {loading ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            Загрузка комнат...
          </div>
        ) : error ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--error-color)" }}>
            {error}
          </div>
        ) : rooms.length === 0 ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            У вас пока нет комнат
          </div>
        ) : (
          rooms.map((room) => (
            <div
              key={room.id}
              className="room-item"
              onClick={() => onNavigate("chat", room.id)}
            >
              <div className="room-avatar">
                <img src={"/placeholder.svg"} alt={room.description || room.name} className="avatar avatar-md" />
              </div>

              <div className="room-info">
                <div className="room-name">{room.description || room.name || "Комната"}</div>
                <div className="room-last-message">
                  Участников: {room.collaborators?.length || 0}
                </div>
              </div>

              <div className="room-meta">
                <div className="room-time">
                  {room.meetingTime ? new Date(room.meetingTime).toLocaleString("ru-RU") : ""}
                </div>
                <div className="room-category">{room.category}</div>
              </div>
            </div>
          ))
        )}
      </div>

      <BottomNavigation currentPage="rooms" onNavigate={onNavigate} />
    </div>
  )
}

export default RoomsList
