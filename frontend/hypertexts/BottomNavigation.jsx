"use client"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/navigation.css"

function BottomNavigation({ currentPage, onNavigate }) {
  return (
    <nav className="bottom-nav">
      <button className={`nav-item ${currentPage === "home" ? "active" : ""}`} onClick={() => onNavigate("home")}>
        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
        <span className="nav-label">Главная</span>
      </button>

      <button className={`nav-item ${currentPage === "rooms" ? "active" : ""}`} onClick={() => onNavigate("rooms")}>
        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        <span className="nav-label">Комнаты</span>
        <span className="nav-badge">3</span>
      </button>

      <button
        className={`nav-item ${currentPage === "createPost" ? "active" : ""}`}
        onClick={() => onNavigate("createPost")}
      >
        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <line x1="12" y1="8" x2="12" y2="16" />
          <line x1="8" y1="12" x2="16" y2="12" />
        </svg>
        <span className="nav-label">Создать пост</span>
      </button>

      <button
        className={`nav-item ${currentPage === "createRoom" ? "active" : ""}`}
        onClick={() => onNavigate("createRoom")}
      >
        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="16" />
          <line x1="8" y1="12" x2="16" y2="12" />
        </svg>
        <span className="nav-label">Создать комнату</span>
      </button>

      <button
        className={`nav-item ${currentPage === "notifications" ? "active" : ""}`}
        onClick={() => onNavigate("notifications")}
      >
        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        <span className="nav-label">Уведомления</span>
      </button>

      <button className={`nav-item ${currentPage === "profile" ? "active" : ""}`} onClick={() => onNavigate("profile")}>
        <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
        <span className="nav-label">Профиль</span>
      </button>
    </nav>
  )
}

export default BottomNavigation
