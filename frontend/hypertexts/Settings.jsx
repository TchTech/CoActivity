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
