"use client"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"
import "../styles/navigation.css"

function RoomInfo({ onNavigate }) {
  // Демо данные комнаты
  const roomData = {
    name: "Физика-механика в Саратове",
    category: "Наука",
    description:
      "Собрание энтузиастов физики и механики. Обсуждаем теоретические вопросы, делимся опытом, проводим эксперименты. Встречи проходят каждую субботу в обсерватории.",
    created: "15 ноября 2024",
    dateTime: "9 декабря 2024, 12:00",
    format: "Очно",
    location: "г. Саратов, ул. Ленина, д. 17",
    type: "По заявкам",
    currentMembers: 8,
    maxMembers: 15,
    owner: {
      name: "Петр Сидоров",
      avatar: "/male-avatar.png",
      rating: 8.5,
    },
    members: [
      { name: "Анна Иванова", avatar: "/female-avatar-1.png" },
      { name: "Сергей Петров", avatar: "/male-avatar-1.png" },
      { name: "Мария Сидорова", avatar: "/female-avatar-2.png" },
      { name: "Алексей Козлов", avatar: "/male-avatar-2.png" },
    ],
  }

  const progressPercent = (roomData.currentMembers / roomData.maxMembers) * 100

  return (
    <div>
      {/* Верхняя навигация */}
      <div className="top-nav">
        <button className="btn-icon" onClick={() => onNavigate("chat")}>
          ←
        </button>
        <div className="top-nav-title">Информация о комнате</div>
        <div style={{ width: "40px" }}></div>
      </div>

      <div className="room-info-container">
        <div className="room-info-card">
          {/* Хедер */}
          <div className="room-info-header">
            <h1 className="room-info-title">{roomData.name}</h1>
            <span className="room-info-category">{roomData.category}</span>
          </div>

          {/* Описание */}
          <div className="room-info-section">
            <div className="room-info-label">Описание</div>
            <p className="room-info-description">{roomData.description}</p>
          </div>

          {/* Дата и время */}
          <div className="room-info-section">
            <div className="room-info-label">Дата и время проведения</div>
            <div className="room-info-value">{roomData.dateTime}</div>
          </div>

          {/* Формат */}
          <div className="room-info-section">
            <div className="room-info-label">Формат</div>
            <div className="room-info-value">{roomData.format}</div>
            {roomData.location && (
              <p
                style={{
                  color: "var(--text-secondary)",
                  marginTop: "var(--spacing-xs)",
                  fontSize: "var(--font-size-base)",
                }}
              >
                📍 {roomData.location}
              </p>
            )}
          </div>

          {/* Тип набора */}
          <div className="room-info-section">
            <div className="room-info-label">Тип набора</div>
            <div className="room-info-value">{roomData.type}</div>
          </div>

          {/* Участники */}
          <div className="room-info-section">
            <div className="room-info-label">Участники</div>
            <div className="room-info-value">
              {roomData.currentMembers} из {roomData.maxMembers}
            </div>
            <div className="room-members-progress">
              <div className="progress-bar">
                <div className="progress-fill" style={{ width: `${progressPercent}%` }}></div>
              </div>
              <div className="progress-text">{roomData.maxMembers - roomData.currentMembers} мест осталось</div>
            </div>
          </div>

          {/* Организатор */}
          <div className="room-info-section">
            <div className="room-info-label">Организатор</div>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "var(--spacing-md)",
                marginTop: "var(--spacing-sm)",
              }}
            >
              <img
                src={roomData.owner.avatar || "/placeholder.svg"}
                alt={roomData.owner.name}
                className="avatar avatar-md"
              />
              <div>
                <div style={{ fontSize: "var(--font-size-base)", fontWeight: "600", color: "var(--text-primary)" }}>
                  {roomData.owner.name}
                </div>
                <div style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)" }}>
                  Рейтинг: {roomData.owner.rating}
                </div>
              </div>
            </div>
          </div>

          {/* Дата создания */}
          <div
            className="room-info-section"
            style={{ paddingTop: "var(--spacing-lg)", borderTop: "1px solid var(--border-color)" }}
          >
            <div className="room-info-label">Комната создана</div>
            <div style={{ color: "var(--text-muted)", fontSize: "var(--font-size-sm)" }}>{roomData.created}</div>
          </div>

          {/* Действия */}
          <div style={{ display: "flex", gap: "var(--spacing-md)", marginTop: "var(--spacing-lg)" }}>
            <button className="btn btn-primary" style={{ flex: 1 }}>
              Пригласить друзей
            </button>
            <button className="btn btn-secondary" style={{ flex: 1 }}>
              Покинуть комнату
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default RoomInfo
