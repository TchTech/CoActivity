"use client"

import { useState } from "react"
import BottomNavigation from "./BottomNavigation"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/navigation.css"

function Settings({ onNavigate }) {
  const [settings, setSettings] = useState({
    soundEnabled: true,
    vibrationEnabled: true,
    notificationsEnabled: true,
    doNotDisturb: false,
    dndStartTime: "22:00",
    dndEndTime: "08:00",
    emailNotifications: true,
    newFollowers: true,
    newComments: true,
    roomInvites: true,
    roomReminders: true,
  })

  const handleToggle = (key) => {
    setSettings((prev) => ({
      ...prev,
      [key]: !prev[key],
    }))
  }

  const handleTimeChange = (key, value) => {
    setSettings((prev) => ({
      ...prev,
      [key]: value,
    }))
  }

  return (
    <div>
      <div className="top-nav">
        <button className="btn-icon" onClick={() => onNavigate("profile")}>
          ←
        </button>
        <div className="top-nav-title">Настройки</div>
        <div style={{ width: "40px" }}></div>
      </div>

      <div style={{ padding: "var(--spacing-lg)", paddingBottom: "80px" }}>
        {/* Уведомления */}
        <div className="settings-section">
          <h3 className="settings-section-title">Уведомления</h3>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Включить уведомления</div>
              <div className="settings-item-description">Получать push-уведомления от CoActivity</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.notificationsEnabled}
                onChange={() => handleToggle("notificationsEnabled")}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Звук</div>
              <div className="settings-item-description">Воспроизводить звук при уведомлениях</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.soundEnabled}
                onChange={() => handleToggle("soundEnabled")}
                disabled={!settings.notificationsEnabled}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Вибрация</div>
              <div className="settings-item-description">Включить вибрацию при уведомлениях</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.vibrationEnabled}
                onChange={() => handleToggle("vibrationEnabled")}
                disabled={!settings.notificationsEnabled}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>
        </div>

        {/* Режим "Не беспокоить" */}
        <div className="settings-section">
          <h3 className="settings-section-title">Режим "Не беспокоить"</h3>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Не беспокоить</div>
              <div className="settings-item-description">Отключить уведомления в определенное время</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.doNotDisturb}
                onChange={() => handleToggle("doNotDisturb")}
                disabled={!settings.notificationsEnabled}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>

          {settings.doNotDisturb && (
            <div className="settings-time-range">
              <div className="settings-time-input">
                <label className="settings-time-label">Начало</label>
                <input
                  type="time"
                  className="input"
                  value={settings.dndStartTime}
                  onChange={(e) => handleTimeChange("dndStartTime", e.target.value)}
                />
              </div>
              <div className="settings-time-input">
                <label className="settings-time-label">Конец</label>
                <input
                  type="time"
                  className="input"
                  value={settings.dndEndTime}
                  onChange={(e) => handleTimeChange("dndEndTime", e.target.value)}
                />
              </div>
            </div>
          )}
        </div>

        {/* Типы уведомлений */}
        <div className="settings-section">
          <h3 className="settings-section-title">Типы уведомлений</h3>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Email уведомления</div>
              <div className="settings-item-description">Получать уведомления на электронную почту</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.emailNotifications}
                onChange={() => handleToggle("emailNotifications")}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Новые подписчики</div>
              <div className="settings-item-description">Когда кто-то подписывается на вас</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.newFollowers}
                onChange={() => handleToggle("newFollowers")}
                disabled={!settings.notificationsEnabled}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Комментарии</div>
              <div className="settings-item-description">Когда кто-то комментирует ваши посты</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.newComments}
                onChange={() => handleToggle("newComments")}
                disabled={!settings.notificationsEnabled}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Приглашения в комнаты</div>
              <div className="settings-item-description">Когда вас приглашают в комнату</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.roomInvites}
                onChange={() => handleToggle("roomInvites")}
                disabled={!settings.notificationsEnabled}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Напоминания о мероприятиях</div>
              <div className="settings-item-description">За час до начала мероприятия</div>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={settings.roomReminders}
                onChange={() => handleToggle("roomReminders")}
                disabled={!settings.notificationsEnabled}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>
        </div>

        {/* Кнопка сохранения */}
        <button
          className="btn btn-primary"
          style={{ width: "100%", marginTop: "var(--spacing-xl)" }}
          onClick={() => {
            console.log("Настройки сохранены:", settings)
            onNavigate("profile")
          }}
        >
          Сохранить настройки
        </button>
      </div>

      <BottomNavigation currentPage="profile" onNavigate={onNavigate} />
    </div>
  )
}

export default Settings
