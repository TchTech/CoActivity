"use client"

import { useState, useEffect } from "react"
import React from "react"
import { userAPI } from "../lib/api"
import { AlertDialog } from "./ui/AlertDialog"

export function ProfileEditSection({
  profileData,
  setProfileData,
  passwordData,
  setPasswordData,
  editingProfile,
  setEditingProfile,
  savingProfile,
  setSavingProfile,
  availableCategories,
  currentUser,
  setShowAlert,
  setAlertData,
  showPasswordDialog,
  setShowPasswordDialog,
}) {
  const [localProfileData, setLocalProfileData] = useState(profileData)
  
  // Sync localProfileData when profileData changes
  React.useEffect(() => {
    setLocalProfileData(profileData)
  }, [profileData])
  
  // Parse name into first and last name
  const parseName = (fullName) => {
    if (!fullName) return { firstName: "", lastName: "" }
    const parts = fullName.trim().split(/\s+/)
    if (parts.length === 1) return { firstName: parts[0], lastName: "" }
    return { firstName: parts[0], lastName: parts.slice(1).join(" ") }
  }
  
  const nameParts = parseName(localProfileData.name)

  const handleToggleInterest = (interestValue) => {
    setLocalProfileData((prev) => {
      const interests = prev.interests || []
      const isSelected = interests.includes(interestValue)
      return {
        ...prev,
        interests: isSelected
          ? interests.filter((i) => i !== interestValue)
          : [...interests, interestValue],
      }
    })
  }

  const handleSaveProfile = async () => {
    if (!currentUser?.id) {
      setAlertData({
        title: "Ошибка",
        message: "Пользователь не авторизован",
        variant: "error",
      })
      setShowAlert(true)
      return
    }

    setSavingProfile(true)
    try {
      // Update name
      if (localProfileData.name !== profileData.name) {
        await userAPI.updateName(currentUser.id, { name: localProfileData.name })
      }

      // Update email
      if (localProfileData.email !== profileData.email) {
        await userAPI.updateEmail(currentUser.id, { email: localProfileData.email })
      }

      // Update about
      if (localProfileData.about !== profileData.about) {
        await userAPI.updateAbout(currentUser.id, { about: localProfileData.about })
      }

      // Update interests
      if (JSON.stringify(localProfileData.interests.sort()) !== JSON.stringify((profileData.interests || []).sort())) {
        await userAPI.updateInterests(currentUser.id, localProfileData.interests)
      }

      setProfileData(localProfileData)
      setEditingProfile(false)
      
      setAlertData({
        title: "Успешно",
        message: "Профиль успешно обновлен",
        variant: "success",
      })
      setShowAlert(true)
    } catch (error) {
      console.error("Ошибка сохранения профиля:", error)
      setAlertData({
        title: "Ошибка",
        message: "Не удалось сохранить изменения: " + (error.message || "Неизвестная ошибка"),
        variant: "error",
      })
      setShowAlert(true)
    } finally {
      setSavingProfile(false)
    }
  }

  const handleCancelEdit = () => {
    setLocalProfileData(profileData)
    setEditingProfile(false)
  }

  // Material Design Input Component
  const MaterialInput = ({ label, value, onChange, type = "text", placeholder, maxLength, disabled }) => (
    <div style={{ marginBottom: "var(--spacing-lg)" }}>
      <label
        style={{
          display: "block",
          fontSize: "var(--font-size-sm)",
          fontWeight: "500",
          color: "var(--text-primary)",
          marginBottom: "var(--spacing-xs)",
        }}
      >
        {label}
      </label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        maxLength={maxLength}
        disabled={disabled || savingProfile}
        style={{
          width: "100%",
          padding: "12px 16px",
          fontSize: "var(--font-size-base)",
          border: `2px solid ${editingProfile ? "var(--accent-gold)" : "var(--border-primary)"}`,
          borderRadius: "8px",
          backgroundColor: disabled ? "var(--bg-tertiary)" : "var(--bg-primary)",
          color: "var(--text-primary)",
          transition: "all 0.2s ease",
          outline: "none",
          boxSizing: "border-box",
        }}
        onFocus={(e) => {
          if (editingProfile) {
            e.target.style.borderColor = "var(--accent-gold)"
            e.target.style.boxShadow = "0 0 0 3px rgba(255, 215, 0, 0.1)"
          }
        }}
        onBlur={(e) => {
          e.target.style.borderColor = editingProfile ? "var(--accent-gold)" : "var(--border-primary)"
          e.target.style.boxShadow = "none"
        }}
      />
    </div>
  )

  // Material Design Textarea Component
  const MaterialTextarea = ({ label, value, onChange, placeholder, maxLength, rows = 4 }) => (
    <div style={{ marginBottom: "var(--spacing-lg)" }}>
      <label
        style={{
          display: "block",
          fontSize: "var(--font-size-sm)",
          fontWeight: "500",
          color: "var(--text-primary)",
          marginBottom: "var(--spacing-xs)",
        }}
      >
        {label}
      </label>
      <textarea
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        maxLength={maxLength}
        rows={rows}
        disabled={!editingProfile || savingProfile}
        style={{
          width: "100%",
          padding: "12px 16px",
          fontSize: "var(--font-size-base)",
          border: `2px solid ${editingProfile ? "var(--accent-gold)" : "var(--border-primary)"}`,
          borderRadius: "8px",
          backgroundColor: !editingProfile ? "var(--bg-tertiary)" : "var(--bg-primary)",
          color: "var(--text-primary)",
          transition: "all 0.2s ease",
          outline: "none",
          resize: "vertical",
          fontFamily: "inherit",
          boxSizing: "border-box",
        }}
        onFocus={(e) => {
          if (editingProfile) {
            e.target.style.borderColor = "var(--accent-gold)"
            e.target.style.boxShadow = "0 0 0 3px rgba(255, 215, 0, 0.1)"
          }
        }}
        onBlur={(e) => {
          e.target.style.borderColor = editingProfile ? "var(--accent-gold)" : "var(--border-primary)"
          e.target.style.boxShadow = "none"
        }}
      />
      {maxLength && (
        <div style={{ fontSize: "var(--font-size-xs)", color: "var(--text-muted)", marginTop: "4px", textAlign: "right" }}>
          {value?.length || 0} / {maxLength}
        </div>
      )}
    </div>
  )

  // Material Design Button
  const MaterialButton = ({ onClick, disabled, variant = "primary", children, style = {} }) => {
    const isPrimary = variant === "primary"
    return (
      <button
        onClick={onClick}
        disabled={disabled}
        style={{
          padding: "12px 24px",
          fontSize: "var(--font-size-base)",
          fontWeight: "600",
          border: isPrimary ? "none" : `2px solid var(--accent-gold)`,
          borderRadius: "8px",
          backgroundColor: isPrimary ? "var(--accent-gold)" : "transparent",
          color: isPrimary ? "var(--bg-primary)" : "var(--accent-gold)",
          cursor: disabled ? "not-allowed" : "pointer",
          transition: "all 0.2s ease",
          opacity: disabled ? 0.6 : 1,
          ...style,
        }}
        onMouseEnter={(e) => {
          if (!disabled) {
            e.target.style.opacity = "0.9"
            e.target.style.transform = "translateY(-1px)"
          }
        }}
        onMouseLeave={(e) => {
          if (!disabled) {
            e.target.style.opacity = "1"
            e.target.style.transform = "translateY(0)"
          }
        }}
      >
        {children}
      </button>
    )
  }

  return (
    <div>
      {/* Личная информация */}
      <div className="settings-section">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "var(--spacing-md)" }}>
          <h3 className="settings-section-title">Личная информация</h3>
          {!editingProfile && (
            <MaterialButton
              onClick={() => setEditingProfile(true)}
              variant="secondary"
              style={{ padding: "8px 16px", fontSize: "var(--font-size-sm)" }}
            >
              ✏️ Редактировать
            </MaterialButton>
          )}
        </div>

        <MaterialInput
          label="Имя"
          value={nameParts.firstName || ""}
          onChange={(e) => {
            const newFirstName = e.target.value
            const fullName = newFirstName.trim() 
              ? (newFirstName + (nameParts.lastName ? " " + nameParts.lastName : "")).trim()
              : nameParts.lastName
            setLocalProfileData({ ...localProfileData, name: fullName })
          }}
          placeholder="Введите ваше имя"
          disabled={!editingProfile}
        />

        <MaterialInput
          label="Фамилия"
          value={nameParts.lastName || ""}
          onChange={(e) => {
            const newLastName = e.target.value
            const fullName = nameParts.firstName.trim()
              ? (nameParts.firstName + (newLastName ? " " + newLastName : "")).trim()
              : newLastName
            setLocalProfileData({ ...localProfileData, name: fullName })
          }}
          placeholder="Введите вашу фамилию"
          disabled={!editingProfile}
        />

        <MaterialInput
          label="Email"
          type="email"
          value={localProfileData.email || ""}
          onChange={(e) => setLocalProfileData({ ...localProfileData, email: e.target.value })}
          placeholder="Введите ваш email"
          disabled={!editingProfile}
        />

        <MaterialTextarea
          label="О себе"
          value={localProfileData.about || ""}
          onChange={(e) => setLocalProfileData({ ...localProfileData, about: e.target.value })}
          placeholder="Расскажите о себе..."
          maxLength={500}
          rows={4}
        />
      </div>

      {/* Интересы */}
      <div className="settings-section">
        <h3 className="settings-section-title">Интересы</h3>
        <div style={{ 
          display: "flex", 
          flexWrap: "wrap", 
          gap: "var(--spacing-sm)",
          marginTop: "var(--spacing-md)",
        }}>
          {availableCategories.map((category) => {
            const isSelected = (localProfileData.interests || []).includes(category.value)
            return (
              <button
                key={category.value}
                onClick={() => editingProfile && handleToggleInterest(category.value)}
                disabled={!editingProfile || savingProfile}
                style={{
                  padding: "8px 16px",
                  fontSize: "var(--font-size-sm)",
                  border: `2px solid ${isSelected ? "var(--accent-gold)" : "var(--border-primary)"}`,
                  borderRadius: "20px",
                  backgroundColor: isSelected ? "var(--accent-gold)" : "transparent",
                  color: isSelected ? "var(--bg-primary)" : "var(--text-primary)",
                  cursor: editingProfile ? "pointer" : "default",
                  transition: "all 0.2s ease",
                  opacity: !editingProfile ? 0.7 : 1,
                  fontWeight: isSelected ? "600" : "400",
                }}
                onMouseEnter={(e) => {
                  if (editingProfile && !isSelected) {
                    e.target.style.borderColor = "var(--accent-gold)"
                    e.target.style.opacity = "0.8"
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isSelected) {
                    e.target.style.borderColor = "var(--border-primary)"
                    e.target.style.opacity = "1"
                  }
                }}
              >
                {category.label}
              </button>
            )
          })}
        </div>
      </div>

      {/* Пароль */}
      <div className="settings-section">
        <h3 className="settings-section-title">Безопасность</h3>
        <MaterialButton
          onClick={() => setShowPasswordDialog(true)}
          variant="secondary"
          style={{ marginTop: "var(--spacing-md)" }}
        >
          🔒 Изменить пароль
        </MaterialButton>
      </div>

      {/* Кнопки сохранения/отмены */}
      {editingProfile && (
        <div style={{ 
          display: "flex", 
          gap: "var(--spacing-md)", 
          marginTop: "var(--spacing-xl)",
          paddingTop: "var(--spacing-lg)",
          borderTop: "2px solid var(--border-primary)",
        }}>
          <MaterialButton
            onClick={handleSaveProfile}
            disabled={savingProfile}
            variant="primary"
            style={{ flex: 1 }}
          >
            {savingProfile ? "Сохранение..." : "Сохранить изменения"}
          </MaterialButton>
          <MaterialButton
            onClick={handleCancelEdit}
            disabled={savingProfile}
            variant="secondary"
            style={{ flex: 1 }}
          >
            Отмена
          </MaterialButton>
        </div>
      )}
    </div>
  )
}

