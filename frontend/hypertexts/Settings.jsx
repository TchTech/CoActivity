"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/navigation.css"
import { userAPI, passwordResetAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { AlertDialog } from "../components/ui/AlertDialog"
import { ConfirmDialog } from "../components/ui/ConfirmDialog"
import { ProfileEditSection } from "../components/ProfileEditSection"

function Settings({ onNavigate, currentPage }) {
  const { currentUser } = useUser()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState("profile") // "profile" or "notifications"
  const [settings, setSettings] = useState({
    notificationsEnabled: true,
    emailNotifications: true,
    newFollowers: true,
    newComments: true,
    roomInvites: true,
    roomReminders: true,
  })
  const [showAlert, setShowAlert] = useState(false)
  const [alertData, setAlertData] = useState({ title: "", message: "", variant: "info" })
  
  // Profile editing state
  const [profileData, setProfileData] = useState({
    name: "",
    email: "",
    about: "",
    interests: [],
  })
  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  })
  const [editingProfile, setEditingProfile] = useState(false)
  const [savingProfile, setSavingProfile] = useState(false)
  const [showPasswordDialog, setShowPasswordDialog] = useState(false)
  
  // Available interests
  const availableCategories = [
    { value: "science", label: "Наука" },
    { value: "it", label: "Программирование" },
    { value: "sport", label: "Спорт" },
    { value: "art", label: "Искусство" },
    { value: "music", label: "Музыка" },
    { value: "books", label: "Книги" },
    { value: "travel", label: "Путешествия" },
    { value: "cooking", label: "Кулинария" },
    { value: "photo", label: "Фотография" },
    { value: "games", label: "Игры" },
  ]

  // Load current settings and profile data
  useEffect(() => {
    const loadData = async () => {
      if (!currentUser?.id) {
        setLoading(false)
        return
      }

      try {
        setLoading(true)
        
        // Load notification settings
        try {
          const data = await userAPI.getNotificationSettings(currentUser.id)
          setSettings({
            notificationsEnabled: data.pushNotifications !== false,
            emailNotifications: data.emailNotifications !== false,
            newFollowers: data.newFollowers !== false,
            newComments: data.newComments !== false,
            roomInvites: data.roomInvites !== false,
            roomReminders: data.roomReminders !== false,
          })
        } catch (error) {
          console.error("Ошибка загрузки настроек уведомлений:", error)
        }
        
        // Load profile data
        try {
          const profile = await userAPI.getProfile(currentUser.id)
          const personalInfo = await userAPI.getPersonalInfo(currentUser.id)
          const aboutData = await userAPI.getAbout(currentUser.id)
          
          setProfileData({
            name: personalInfo?.name || profile?.name || "",
            email: personalInfo?.email || profile?.email || "",
            about: aboutData?.about || profile?.about || "",
            interests: profile?.interests?.map(i => typeof i === "string" ? i : i.name || i) || [],
          })
        } catch (error) {
          console.error("Ошибка загрузки профиля:", error)
        }
      } catch (error) {
        console.error("Ошибка загрузки данных:", error)
        setAlertData({
          title: "Ошибка",
          message: "Не удалось загрузить данные",
          variant: "error"
        })
        setShowAlert(true)
      } finally {
        setLoading(false)
      }
    }

    loadData()
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
        newFollowers: settings.newFollowers,
        newComments: settings.newComments,
        roomInvites: settings.roomInvites,
        roomReminders: settings.roomReminders,
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

      {/* Tabs */}
      <div style={{ 
        display: "flex", 
        borderBottom: "2px solid var(--border-primary)",
        marginBottom: "var(--spacing-md)"
      }}>
        <button
          onClick={() => setActiveTab("profile")}
          style={{
            flex: 1,
            padding: "var(--spacing-md)",
            background: "none",
            border: "none",
            borderBottom: activeTab === "profile" ? "3px solid var(--accent-gold)" : "3px solid transparent",
            color: activeTab === "profile" ? "var(--accent-gold)" : "var(--text-muted)",
            fontWeight: activeTab === "profile" ? "600" : "400",
            cursor: "pointer",
            transition: "all 0.2s ease",
            fontSize: "var(--font-size-base)",
          }}
        >
          Профиль
        </button>
        <button
          onClick={() => setActiveTab("notifications")}
          style={{
            flex: 1,
            padding: "var(--spacing-md)",
            background: "none",
            border: "none",
            borderBottom: activeTab === "notifications" ? "3px solid var(--accent-gold)" : "3px solid transparent",
            color: activeTab === "notifications" ? "var(--accent-gold)" : "var(--text-muted)",
            fontWeight: activeTab === "notifications" ? "600" : "400",
            cursor: "pointer",
            transition: "all 0.2s ease",
            fontSize: "var(--font-size-base)",
          }}
        >
          Уведомления
        </button>
      </div>

      <div style={{ padding: "var(--spacing-lg)", paddingBottom: "80px" }}>
        {loading ? (
          <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--text-muted)" }}>
            Загрузка данных...
          </div>
        ) : activeTab === "profile" ? (
          <ProfileEditSection
            profileData={profileData}
            setProfileData={setProfileData}
            passwordData={passwordData}
            setPasswordData={setPasswordData}
            editingProfile={editingProfile}
            setEditingProfile={setEditingProfile}
            savingProfile={savingProfile}
            setSavingProfile={setSavingProfile}
            availableCategories={availableCategories}
            currentUser={currentUser}
            setShowAlert={setShowAlert}
            setAlertData={setAlertData}
            showPasswordDialog={showPasswordDialog}
            setShowPasswordDialog={setShowPasswordDialog}
          />
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

            {/* Типы уведомлений */}
            <div className="settings-section">
              <h3 className="settings-section-title">Типы уведомлений</h3>

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

      {/* Password Change Dialog */}
      <ConfirmDialog
        open={showPasswordDialog}
        title="Изменение пароля"
        message="Для изменения пароля будет отправлено письмо на ваш email с инструкциями."
        confirmText="Отправить письмо"
        cancelText="Отмена"
        onConfirm={async () => {
          try {
            // Используем API для запроса сброса пароля
            await passwordResetAPI.request(profileData.email)
            setShowPasswordDialog(false)
            setAlertData({
              title: "Успешно",
              message: "Письмо с инструкциями отправлено на ваш email",
              variant: "success",
            })
            setShowAlert(true)
          } catch (error) {
            console.error("Ошибка запроса сброса пароля:", error)
            setAlertData({
              title: "Ошибка",
              message: "Не удалось отправить письмо: " + (error.message || "Неизвестная ошибка"),
              variant: "error",
            })
            setShowAlert(true)
          }
        }}
        onCancel={() => setShowPasswordDialog(false)}
      />
    </div>
  )
}

export default Settings
