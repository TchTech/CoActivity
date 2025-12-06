"use client"
import { useEffect, useState } from "react"
import BottomNavigation from "./BottomNavigation"
import { roomAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { getAvatarEmoji } from "../utils/avatarUtils"
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
  const [refreshKey, setRefreshKey] = useState(0)
  const [filters, setFilters] = useState({
    category: "",
    startDate: "",
    endDate: "",
    location: ""
  })
  const [showFilters, setShowFilters] = useState(false)
  
  // Available categories (matching CreateRoom.jsx)
  const categories = [
    { value: "", label: "Все категории" },
    { value: "science", label: "Наука" },
    { value: "it", label: "Программирование" },
    { value: "sport", label: "Спорт" },
    { value: "art", label: "Искусство" },
    { value: "music", label: "Музыка" },
    { value: "books", label: "Книги" },
    { value: "travel", label: "Путешествия" },
    { value: "cooking", label: "Кулинария" },
    { value: "photo", label: "Фотография" },
    { value: "games", label: "Игры" }
  ]

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

  // Load rooms for "mine" tab
  useEffect(() => {
    if (activeTab !== "mine") {
      return
    }
    
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
        let roomsArray = Array.isArray(data) ? data : (data ? [data] : [])
        
        // Filter by search query if present
        if (searchQuery.trim().length > 0) {
          const query = searchQuery.trim().toLowerCase()
          roomsArray = roomsArray.filter(room => 
            (room.name && room.name.toLowerCase().includes(query)) ||
            (room.description && room.description.toLowerCase().includes(query))
          )
        }
        
        // Sort rooms by creation date (newest first)
        const sortedRooms = roomsArray.length > 0
          ? roomsArray.sort((a, b) => {
              const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
              const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
              return dateB - dateA
            })
          : []
        console.log("[RoomsList] Sorted rooms:", sortedRooms.length)
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
  }, [currentUser, activeTab, searchQuery, refreshKey])

  // Search rooms when query or filters change (only for "all" tab)
  useEffect(() => {
    if (activeTab !== "all") {
      return // Don't search when on "mine" tab
    }
    
    const hasSearchQuery = searchQuery.trim().length > 0
    const hasFilters = filters.category || filters.startDate || filters.endDate || filters.location
    
    if (hasSearchQuery || hasFilters) {
      const searchRooms = async () => {
        setIsSearching(true)
        setError("")
        try {
          // Prepare filter parameters
          const searchFilters = {}
          if (filters.category) {
            searchFilters.category = filters.category
          }
          if (filters.startDate) {
            // Convert to ISO string for backend
            const startDate = new Date(filters.startDate)
            startDate.setHours(0, 0, 0, 0) // Start of day
            searchFilters.startDate = startDate.toISOString()
          }
          if (filters.endDate) {
            // Convert to ISO string for backend
            const endDate = new Date(filters.endDate)
            endDate.setHours(23, 59, 59, 999) // End of day
            searchFilters.endDate = endDate.toISOString()
          }
          if (filters.location) {
            searchFilters.location = filters.location
          }
          
          console.log("[RoomsList] Searching rooms with query:", searchQuery.trim(), "filters:", searchFilters)
          const data = await roomAPI.search(searchQuery.trim() || undefined, searchFilters)
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
      // If no search query and no filters, reload all rooms
      const loadRooms = async () => {
        setLoading(true)
        setError("")
        try {
          console.log("[RoomsList] Loading all rooms")
          const data = await roomAPI.getAllRooms(0, 50)
          console.log("[RoomsList] Loaded rooms:", data)
          
          const roomsArray = Array.isArray(data) ? data : (data ? [data] : [])
          const sortedRooms = roomsArray.length > 0
            ? roomsArray.sort((a, b) => {
                const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
                const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
                return dateB - dateA
              })
            : []
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
  }, [searchQuery, filters, activeTab, refreshKey])
  
  // Reset filters when switching tabs
  useEffect(() => {
    if (activeTab === "mine") {
      setFilters({ category: "", startDate: "", endDate: "", location: "" })
      setSearchQuery("")
      setShowFilters(false)
    }
  }, [activeTab])


  return (
    <div>
      {/* Верхняя навигация */}
      <div className="top-nav">
        <div className="top-nav-title">Комнаты</div>
        <div className="top-nav-actions">
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

      {/* Поиск и фильтры */}
      {activeTab === "all" && (
        <div style={{ padding: "var(--spacing-md)", paddingBottom: "var(--spacing-sm)" }}>
          <div style={{ display: "flex", gap: "var(--spacing-sm)", marginBottom: "var(--spacing-sm)" }}>
            <input
              type="text"
              placeholder="Поиск по названию, описанию или локации..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                flex: 1,
                padding: "var(--spacing-sm) var(--spacing-md)",
                borderRadius: "var(--radius-md)",
                border: "1px solid var(--border-color)",
                fontSize: "var(--font-size-base)",
                backgroundColor: "var(--bg-secondary)",
                color: "var(--text-primary)",
              }}
            />
            <button
              onClick={() => setShowFilters(!showFilters)}
              style={{
                padding: "var(--spacing-sm) var(--spacing-md)",
                borderRadius: "var(--radius-md)",
                border: "1px solid var(--border-color)",
                backgroundColor: showFilters ? "var(--accent-blue)" : "var(--bg-secondary)",
                color: showFilters ? "white" : "var(--text-primary)",
                cursor: "pointer",
                fontSize: "var(--font-size-sm)",
                fontWeight: "500",
                whiteSpace: "nowrap"
              }}
            >
              {showFilters ? "Скрыть фильтры" : "Фильтры"}
            </button>
          </div>
          
          {showFilters && (
            <div style={{
              padding: "var(--spacing-md)",
              backgroundColor: "var(--bg-tertiary)",
              borderRadius: "var(--radius-md)",
              border: "1px solid var(--border-color)",
              display: "flex",
              flexDirection: "column",
              gap: "var(--spacing-md)"
            }}>
              <div>
                <label style={{
                  display: "block",
                  marginBottom: "var(--spacing-xs)",
                  fontSize: "var(--font-size-sm)",
                  fontWeight: "500",
                  color: "var(--text-primary)"
                }}>
                  Категория интересов
                </label>
                <select
                  value={filters.category}
                  onChange={(e) => setFilters({ ...filters, category: e.target.value })}
                  style={{
                    width: "100%",
                    padding: "var(--spacing-sm) var(--spacing-md)",
                    borderRadius: "var(--radius-md)",
                    border: "1px solid var(--border-color)",
                    fontSize: "var(--font-size-base)",
                    backgroundColor: "var(--bg-secondary)",
                    color: "var(--text-primary)",
                  }}
                >
                  {categories.map((cat) => (
                    <option key={cat.value} value={cat.value}>
                      {cat.label}
                    </option>
                  ))}
                </select>
              </div>
              
              <div>
                <label style={{
                  display: "block",
                  marginBottom: "var(--spacing-xs)",
                  fontSize: "var(--font-size-sm)",
                  fontWeight: "500",
                  color: "var(--text-primary)"
                }}>
                  Дата начала (от)
                </label>
                <input
                  type="date"
                  value={filters.startDate}
                  onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
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
              
              <div>
                <label style={{
                  display: "block",
                  marginBottom: "var(--spacing-xs)",
                  fontSize: "var(--font-size-sm)",
                  fontWeight: "500",
                  color: "var(--text-primary)"
                }}>
                  Дата окончания (до)
                </label>
                <input
                  type="date"
                  value={filters.endDate}
                  onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                  min={filters.startDate || undefined}
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
              
              <div>
                <label style={{
                  display: "block",
                  marginBottom: "var(--spacing-xs)",
                  fontSize: "var(--font-size-sm)",
                  fontWeight: "500",
                  color: "var(--text-primary)"
                }}>
                  Локация (город, адрес)
                </label>
                <input
                  type="text"
                  placeholder="Введите город или адрес..."
                  value={filters.location}
                  onChange={(e) => setFilters({ ...filters, location: e.target.value })}
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
              
              {(filters.category || filters.startDate || filters.endDate || filters.location) && (
                <button
                  onClick={() => setFilters({ category: "", startDate: "", endDate: "", location: "" })}
                  style={{
                    padding: "var(--spacing-sm) var(--spacing-md)",
                    borderRadius: "var(--radius-md)",
                    border: "1px solid var(--border-color)",
                    backgroundColor: "var(--error-color)",
                    color: "white",
                    cursor: "pointer",
                    fontSize: "var(--font-size-sm)",
                    fontWeight: "500"
                  }}
                >
                  Сбросить фильтры
                </button>
              )}
            </div>
          )}
        </div>
      )}
      
      {/* Поиск для вкладки "Мои комнаты" */}
      {activeTab === "mine" && (
        <div style={{ padding: "var(--spacing-md)", paddingBottom: "var(--spacing-sm)" }}>
          <input
            type="text"
            placeholder="Поиск по названию, описанию или локации..."
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
      )}

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
                {(() => {
                  // Получаем аватарку создателя комнаты
                  const creatorAvatarId = room.createdBy?.avatar?.id
                  const hasCreatorAvatar = creatorAvatarId != null && creatorAvatarId !== undefined && creatorAvatarId !== 0 && creatorAvatarId !== ""
                  const roomId = typeof room.id === "object" ? (room.id?.id || room.id?.roomId || null) : room.id
                  
                  if (hasCreatorAvatar) {
                    try {
                      const avatarUrl = imageAPI.getImageUrl(creatorAvatarId)
                      return (
                        <img 
                          src={avatarUrl} 
                          alt={room.createdBy?.name || room.createdBy?.username || room.name || "Создатель"} 
                          className="avatar avatar-md avatar-clickable"
                          onClick={(e) => {
                            e.stopPropagation()
                            if (roomId) {
                              onNavigate("roomInfo", roomId)
                            }
                          }}
                          onError={(e) => {
                            // Silently hide broken images
                            e.target.style.display = 'none'
                          }}
                        />
                      )
                    } catch (err) {
                      // If getImageUrl fails, show emoji avatar
                      return (
                        <div
                          className="avatar avatar-md avatar-clickable"
                          style={{
                            backgroundColor: "var(--bg-tertiary)",
                            border: "1px solid var(--border-color)",
                            width: "40px",
                            height: "40px",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontSize: "var(--font-size-base)",
                            cursor: "pointer"
                          }}
                          onClick={(e) => {
                            e.stopPropagation()
                            if (roomId) {
                              onNavigate("roomInfo", roomId)
                            }
                          }}
                        >
                          {getAvatarEmoji(room.createdBy?.id)}
                        </div>
                      )
                    }
                  } else {
                    return (
                      <div
                        className="avatar avatar-md avatar-clickable"
                        style={{
                          backgroundColor: "var(--bg-tertiary)",
                          border: "1px solid var(--border-color)",
                          width: "40px",
                          height: "40px",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          fontSize: "var(--font-size-base)",
                          cursor: "pointer"
                        }}
                        onClick={(e) => {
                          e.stopPropagation()
                          if (roomId) {
                            onNavigate("roomInfo", roomId)
                          }
                        }}
                      >
                        {getAvatarEmoji(room.createdBy?.id)}
                      </div>
                    )
                  }
                })()}
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
