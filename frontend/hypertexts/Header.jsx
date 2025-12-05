import "../styles/header.css"

/**
 * Компонент шапки приложения
 */
function Header() {
  return (
    <header className="app-header">
      <div className="header-content">
        <button className="menu-button" aria-label="Меню">
          ☰
        </button>

        <div className="app-logo">CoActivity</div>

        <div className="header-actions">
          {/* Уведомления теперь только в RoomsList */}
        </div>
      </div>
    </header>
  )
}

export default Header
