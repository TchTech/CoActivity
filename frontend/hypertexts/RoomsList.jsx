"use client"
import { useEffect, useState } from "react"
import { Bell } from "lucide-react"
import BottomNavigation from "./BottomNavigation"
import { roomAPI, notificationAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import NotificationsPanel from "../components/NotificationsPanel"
import { Popover, PopoverTrigger, PopoverContent } from "../components/ui/popover"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"
import "../styles/navigation.css"

function RoomsList({ onNavigate, currentPage }) {
  const { currentUser } = useUser()
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [searchQuery, setSearchQuery] = useState("")
  const [isSearching, setIsSearching] = useState(false)
  const [activeTab, setActiveTab] = useState("all") // "all" or "mine"
  const [unreadCount, setUnreadCount] = useState(0)
  const [notificationPanelOpen, setNotificationPanelOpen] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  // Function to refresh room list (can be called from child components)
  const refreshRoomList = () => {
    setRefreshKey(prev => prev + 1)
  }

  // Listen for room update events
  useEffect(() => {
    const handleRoomUpdated = () => {
      refreshRoomList()
    }
    
    window.addEventListener('roomUpdated', handleRoomUpdated)
    return () => {
      window.removeEventListener('roomUpdated', handleRoomUpdated)
    }
  }, [])

  useEffect(() => {
    const loadRooms = async () => {
      if (!currentUser?.id && activeTab === "mine") {
        console.log("[RoomsList] No currentUser.id, skipping load")
        setLoading(false)
        return
      }

      setLoading(true)
      setError("")
      try {
        let data
        if (activeTab === "all") {
          console.log("[RoomsList] Loading all rooms")
          data = await roomAPI.getAllRooms(0, 50)
        } else {
          console.log("[RoomsList] Loading rooms for user:", currentUser.id)
          data = await roomAPI.getUserRooms(currentUser.id)
        }
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
  }, [currentUser, activeTab])

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
  }, [searchQuery, currentUser, refreshKey])

  // Load unread notification count
  const refreshUnreadCount = async () => {
    if (!currentUser?.id) {
      setUnreadCount(0)
      return
    }

    try {
      const countData = await notificationAPI.getUnreadCount(currentUser.id)
      setUnreadCount(countData?.count || 0)
    } catch (error) {
      console.error("Ошибка загрузки количества уведомлений:", error)
      setUnreadCount(0)
    }
  }

  useEffect(() => {
    if (!currentUser?.id) {
      setUnreadCount(0)
      return
    }

    refreshUnreadCount()
    const interval = setInterval(refreshUnreadCount, 30000) // Poll every 30s
    return () => clearInterval(interval)
  }, [currentUser])

  return (
    <div>
      {/* Верхняя навигация */}
      <div className="top-nav">
        <div className="top-nav-title">Комнаты</div>
        <div className="top-nav-actions" style={{ display: "flex", gap: "var(--spacing-sm)", alignItems: "center" }}>
          <button className="btn-icon" onClick={() => onNavigate("createRoom")}>
            +
          </button>
          
          {/* Notification Bell */}
          {currentUser?.id && (
            <Popover open={notificationPanelOpen} onOpenChange={setNotificationPanelOpen}>
              <PopoverTrigger asChild>
                <button
                  className="btn-icon"
                  style={{ 
                    position: "relative",
                    width: "40px",
                    height: "40px"
                  }}
                  aria-label="Уведомления"
                >
                  <Bell size={20} style={{ color: "var(--text-primary)" }} />
                  {unreadCount > 0 && (
                    <span
                      style={{
                        position: "absolute",
                        top: "-4px",
                        right: "-4px",
                        backgroundColor: "var(--accent-gold)",
                        color: "var(--bg-primary)",
                        borderRadius: "50%",
                        width: "20px",
                        height: "20px",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "11px",
                        fontWeight: "700",
                        border: "2px solid var(--bg-primary)",
                        boxShadow: "0 0 8px rgba(212, 175, 55, 0.6)",
                      }}
                    >
                      {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                  )}
                </button>
              </PopoverTrigger>
              <PopoverContent
                side="bottom"
                align="end"
                style={{
                  padding: 0,
                  width: "380px",
                  maxHeight: "600px",
                  overflow: "hidden",
                  backgroundColor: "transparent",
                  border: "none",
                  boxShadow: "none",
                }}
              >
                <NotificationsPanel
                  userId={currentUser.id}
                  onClose={() => setNotificationPanelOpen(false)}
                  onNavigate={onNavigate}
                  onNotificationUpdate={refreshUnreadCount}
                />
              </PopoverContent>
            </Popover>
          )}

          {/* Profile Avatar - Navigate to profile */}
          {currentUser?.id && (
            <button
              className="btn-icon"
              onClick={() => onNavigate("profile", currentUser.id)}
              style={{
                width: "32px",
                height: "32px",
                borderRadius: "50%",
                padding: 0,
                overflow: "hidden",
                border: "2px solid var(--border-color)",
              }}
              aria-label="Профиль"
            >
              {currentUser.avatar?.id ? (
                <img
                  src={imageAPI.getImageUrl(currentUser.avatar.id)}
                  alt={currentUser.name || "Профиль"}
                  style={{
                    width: "100%",
                    height: "100%",
                    objectFit: "cover",
                  }}
                />
              ) : (
                <div
                  style={{
                    width: "100%",
                    height: "100%",
                    backgroundColor: "var(--accent-blue)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "white",
                    fontSize: "var(--font-size-sm)",
                    fontWeight: "600",
                  }}
                >
                  {currentUser.name?.[0]?.toUpperCase() || currentUser.username?.[0]?.toUpperCase() || "?"}
                </div>
              )}
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        <button 
          className={`tab ${activeTab === "all" ? "active" : ""}`} 
          onClick={() => setActiveTab("all")}
        >
          Все комнаты
        </button>
        <button 
          className={`tab ${activeTab === "mine" ? "active" : ""}`} 
          onClick={() => setActiveTab("mine")}
        >
          Мои комнаты
        </button>
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
              style={{
                opacity: room.isClosed ? 0.7 : 1,
                borderLeft: room.isClosed ? "3px solid var(--error-color)" : "none"
              }}
              onClick={() => {
                const roomId = typeof room.id === "object" ? (room.id?.id || room.id?.roomId || null) : room.id
                if (roomId) {
                  onNavigate("roomInfo", roomId)
                } else {
                  console.error("[RoomsList] Invalid room.id:", room.id)
                }
              }}
            >
              <div className="room-avatar">
                <img src={"/placeholder.svg"} alt={room.description || room.name} className="avatar avatar-md" />
              </div>

              <div className="room-info">
                <div style={{ display: "flex", alignItems: "center", gap: "var(--spacing-xs)" }}>
                  <div className="room-name" style={{ 
                    opacity: room.isClosed ? 0.6 : 1,
                    flex: 1
                  }}>
                    {room.name || room.description || "Комната"}
                  </div>
                  {room.isClosed && (
                    <span className="badge" style={{
                      backgroundColor: "var(--error-color)",
                      color: "white",
                      fontWeight: "600",
                      textTransform: "uppercase"
                    }}>
                      Закрыта
                    </span>
                  )}
                </div>
                <div className="room-last-message" style={{ 
                  opacity: room.isClosed ? 0.6 : 1 
                }}>
                  {room.description && room.description.length > 50 
                    ? room.description.substring(0, 50) + "..." 
                    : room.description || ""}
                </div>
                <div style={{ 
                  fontSize: "var(--font-size-sm)", 
                  color: room.isClosed ? "var(--text-muted)" : "var(--text-muted)", 
                  marginTop: "var(--spacing-xs)",
                  opacity: room.isClosed ? 0.6 : 1
                }}>
                  Участников: {room.collaborators?.length || room.memberCount || 0}
                  {room.joinType === "by_application" && (
                    <span style={{ marginLeft: "var(--spacing-sm)" }}>• По заявкам</span>
                  )}
                  {room.joinType === "open" && (
                    <span style={{ marginLeft: "var(--spacing-sm)" }}>• Открытая</span>
                  )}
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

      <BottomNavigation currentPage={currentPage || "rooms"} onNavigate={onNavigate} />
    </div>
  )
}

export default RoomsList
