"use client"
import BottomNavigation from "./BottomNavigation"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"
import "../styles/navigation.css"

function RoomsList({ onNavigate }) {
  // Демо данные комнат
  const rooms = [
    {
      id: 1,
      name: "Физика-механика в Саратове",
      lastMessage: "Петр: Не забудьте взять с собой блокноты!",
      time: "14:30",
      avatar: "/physics-icon.jpg",
      category: "Наука",
      unread: 3,
    },
    {
      id: 2,
      name: "Программисты Москвы",
      lastMessage: "Анна: Кто будет на хакатоне?",
      time: "Вчера",
      avatar: "/coding-icon.jpg",
      category: "IT",
      unread: 0,
    },
    {
      id: 3,
      name: "Настольный теннис - выходные",
      lastMessage: "Вы: Отлично, жду встречи!",
      time: "15 дек",
      avatar: "/table-tennis-match.png",
      category: "Спорт",
      unread: 0,
    },
    {
      id: 4,
      name: "Робототехника для начинающих",
      lastMessage: "Сергей: Завтра будет мастер-класс",
      time: "10 дек",
      avatar: "/robot-icon.png",
      category: "Технологии",
      unread: 1,
    },
  ]

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
        {rooms.map((room) => (
          <div key={room.id} className="room-item" onClick={() => onNavigate("chat")}>
            <div className="room-avatar">
              <img src={room.avatar || "/placeholder.svg"} alt={room.name} className="avatar avatar-md" />
              {room.unread > 0 && <div className="room-unread">{room.unread}</div>}
            </div>

            <div className="room-info">
              <div className="room-name">{room.name}</div>
              <div className="room-last-message">{room.lastMessage}</div>
            </div>

            <div className="room-meta">
              <div className="room-time">{room.time}</div>
              <div className="room-category">{room.category}</div>
            </div>
          </div>
        ))}
      </div>

      <BottomNavigation currentPage="rooms" onNavigate={onNavigate} />
    </div>
  )
}

export default RoomsList
