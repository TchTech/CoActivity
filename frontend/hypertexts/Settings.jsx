"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/navigation.css"
import { userAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { AlertDialog } from "../components/ui/AlertDialog"

function Settings({ onNavigate, currentPage }) {
  const { currentUser } = useUser()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [settings, setSettings] = useState({
    notificationsEnabled: true, // pushNotifications
    emailNotifications: true,
  })
  const [showAlert, setShowAlert] = useState(false)
  const [alertData, setAlertData] = useState({ title: "", message: "", variant: "info" })

  // Load current settings
  useEffect(() => {
    const loadSettings = async () => {
      if (!currentUser?.id) {
        setLoading(false)
        return
      }

      try {
        setLoading(true)
        const data = await userAPI.getNotificationSettings(currentUser.id)
        setSettings({
          notificationsEnabled: data.pushNotifications !== false, // Default to true if null
          emailNotifications: data.emailNotifications !== false, // Default to true if null
        })
      } catch (error) {
        console.error("Ошибка загрузки настроек:", error)
        setAlertData({
          title: "Ошибка",
          message: "Не удалось загрузить настройки уведомлений",
          variant: "error"
        })
        setShowAlert(true)
      } finally {
        setLoading(false)
      }
    }

    loadSettings()
  }, [currentUser])

  const handleToggle = (key) => {
    setSettings((prev) => ({
      ...prev,
      [key]: !prev[key],
    }))
  }

  const handleSave = async () => {
    if (!currentUser?.id) {
      setAlertData({
        title: "Ошибка",
        message: "Пользователь не авторизован",
        variant: "error"
      })
      setShowAlert(true)
      return
    }

    setSaving(true)
    try {
      await userAPI.updateNotificationSettings(currentUser.id, {
        pushNotifications: settings.notificationsEnabled,
        emailNotifications: settings.emailNotifications,
      })
      
      setAlertData({
        title: "Успешно",
        message: "Настройки уведомлений сохранены",
        variant: "success"
      })
      setShowAlert(true)
      
      // Navigate back after a short delay
      setTimeout(() => {
        onNavigate("profile")
      }, 1500)
    } catch (error) {
      console.error("Ошибка сохранения настроек:", error)
      setAlertData({
        title: "Ошибка",
        message: "Не удалось сохранить настройки: " + (error.message || "Неизвестная ошибка"),
        variant: "error"
      })
      setShowAlert(true)
    } finally {
      setSaving(false)
    }
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
        {loading ? (
          <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--text-muted)" }}>
            Загрузка настроек...
          </div>
        ) : (
          <>
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
            </div>

<<<<<<< HEAD
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

        {/* Безопасность */}
        <div className="settings-section">
          <h3 className="settings-section-title">Безопасность</h3>

          <div className="settings-item">
            <div className="settings-item-info">
              <div className="settings-item-label">Двухфакторная аутентификация</div>
              <div className="settings-item-description">Дополнительная защита вашего аккаунта</div>
            </div>
            <button
              className="btn btn-secondary"
              style={{ padding: "0.5rem 1rem" }}
              onClick={() => onNavigate("two-factor-settings")}
            >
              Настроить
            </button>
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
=======
            {/* Кнопка сохранения */}
            <button
              className="btn btn-primary"
              style={{ width: "100%", marginTop: "var(--spacing-xl)" }}
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? "Сохранение..." : "Сохранить настройки"}
            </button>
          </>
        )}
>>>>>>> 1b0a3d75a995fe828bc4439138d7c9d130132b51
      </div>

      <BottomNavigation currentPage={currentPage || "profile"} onNavigate={onNavigate} />

      {/* Alert Dialog */}
      <AlertDialog
        open={showAlert}
        title={alertData.title}
        message={alertData.message}
        variant={alertData.variant}
        onClose={() => setShowAlert(false)}
      />
    </div>
  )
}

export default Settings
