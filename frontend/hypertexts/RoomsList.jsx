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
  const [searchQuery, setSearchQuery] = useState("")
  const [isSearching, setIsSearching] = useState(false)

  useEffect(() => {
    const loadRooms = async () => {
      if (!currentUser?.id) {
        console.log("[RoomsList] No currentUser.id, skipping load")
        setLoading(false)
        return
      }

      setLoading(true)
      setError("")
      try {
        console.log("[RoomsList] Loading rooms for user:", currentUser.id)
        const data = await roomAPI.getUserRooms(currentUser.id)
        console.log("[RoomsList] Loaded rooms from API:", data)
        console.log("[RoomsList] Rooms type:", typeof data, "isArray:", Array.isArray(data), "length:", Array.isArray(data) ? data.length : 'N/A')
        
        // Ensure we have an array
        const roomsArray = Array.isArray(data) ? data : (data ? [data] : [])
        console.log("[RoomsList] Rooms array after normalization:", roomsArray.length)
        
        // Sort rooms by creation date (newest first)
        const sortedRooms = roomsArray.length > 0
          ? roomsArray.sort((a, b) => {
              const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
              const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
              return dateB - dateA
            })
          : []
        console.log("[RoomsList] Sorted rooms:", sortedRooms.length)
        if (sortedRooms.length > 0) {
          console.log("[RoomsList] First room sample:", sortedRooms[0])
        }
        setRooms(sortedRooms)
      } catch (err) {
        console.error("Ошибка загрузки комнат:", err)
        setError(err.message || "Не удалось загрузить комнаты")
        setRooms([])
      } finally {
        setLoading(false)
      }
    }

    loadRooms()
  }, [currentUser])

  // Search rooms when query changes
  useEffect(() => {
    if (searchQuery.trim().length > 0) {
      const searchRooms = async () => {
        setIsSearching(true)
        try {
          console.log("[RoomsList] Searching rooms with query:", searchQuery.trim())
          const data = await roomAPI.search(searchQuery.trim())
          console.log("[RoomsList] Search results:", data)
          console.log("[RoomsList] Search results type:", typeof data, "isArray:", Array.isArray(data), "length:", Array.isArray(data) ? data.length : 'N/A')
          
          // Ensure we have an array
          const roomsArray = Array.isArray(data) ? data : (data ? [data] : [])
          console.log("[RoomsList] Search rooms array after normalization:", roomsArray.length)
          
          const sortedRooms = roomsArray.length > 0
            ? roomsArray.sort((a, b) => {
                const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
                const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
                return dateB - dateA
              })
            : []
          console.log("[RoomsList] Sorted search results:", sortedRooms.length)
          if (sortedRooms.length > 0) {
            console.log("[RoomsList] First search result sample:", sortedRooms[0])
          }
          setRooms(sortedRooms)
        } catch (err) {
          console.error("Ошибка поиска комнат:", err)
          setError(err.message || "Ошибка при поиске комнат")
          setRooms([])
        } finally {
          setIsSearching(false)
        }
      }
      const timeoutId = setTimeout(searchRooms, 300) // Debounce search
      return () => clearTimeout(timeoutId)
    } else {
      // If search is empty, load user's rooms
      if (currentUser?.id) {
        const loadRooms = async () => {
          setLoading(true)
          setError("")
          try {
            console.log("[RoomsList] Reloading rooms for user:", currentUser.id)
            const data = await roomAPI.getUserRooms(currentUser.id)
            console.log("[RoomsList] Reloaded rooms:", data)
            console.log("[RoomsList] Reloaded rooms type:", typeof data, "isArray:", Array.isArray(data), "length:", Array.isArray(data) ? data.length : 'N/A')
            
            // Ensure we have an array
            const roomsArray = Array.isArray(data) ? data : (data ? [data] : [])
            console.log("[RoomsList] Reloaded rooms array after normalization:", roomsArray.length)
            
            const sortedRooms = roomsArray.length > 0
              ? roomsArray.sort((a, b) => {
                  const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
                  const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
                  return dateB - dateA
                })
              : []
            console.log("[RoomsList] Reloaded sorted rooms:", sortedRooms.length)
            if (sortedRooms.length > 0) {
              console.log("[RoomsList] First reloaded room sample:", sortedRooms[0])
            }
            setRooms(sortedRooms)
          } catch (err) {
            console.error("Ошибка загрузки комнат:", err)
            setError(err.message || "Не удалось загрузить комнаты")
            setRooms([])
          } finally {
            setLoading(false)
          }
        }
        loadRooms()
      }
    }
  }, [searchQuery, currentUser])

  return (
    <div>
      {/* Верхняя навигация */}
      <div className="top-nav">
        <div className="top-nav-title">Мои комнаты</div>
        <div className="top-nav-actions">
          <button className="btn-icon" onClick={() => onNavigate("createRoom")}>
            +
          </button>
        </div>
      </div>

      {/* Поиск */}
      <div style={{ padding: "var(--spacing-md)", paddingBottom: "var(--spacing-sm)" }}>
        <input
          type="text"
          placeholder="Поиск комнат по названию..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{
            width: "100%",
            padding: "var(--spacing-sm) var(--spacing-md)",
            borderRadius: "var(--radius-md)",
            border: "1px solid var(--border-color)",
            fontSize: "var(--font-size-base)",
            backgroundColor: "var(--bg-secondary)",
            color: "var(--text-primary)",
          }}
        />
      </div>

      <div className="rooms-list-container">
        {(loading || isSearching) ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            {isSearching ? "Поиск..." : "Загрузка комнат..."}
          </div>
        ) : error ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--error-color)" }}>
            {error}
          </div>
        ) : rooms.length === 0 ? (
          <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
            {searchQuery.trim().length > 0 ? "Комнаты не найдены" : "У вас пока нет комнат"}
          </div>
        ) : (
          rooms.map((room) => (
            <div
              key={room.id}
              className="room-item"
              onClick={() => {
                const roomId = typeof room.id === "object" ? (room.id?.id || room.id?.roomId || null) : room.id
                if (roomId) {
                  onNavigate("chat", roomId)
                } else {
                  console.error("[RoomsList] Invalid room.id:", room.id)
                }
              }}
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
