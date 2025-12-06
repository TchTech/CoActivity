"use client"

import { useState, useEffect } from "react"
import { twoFactorAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { ConfirmDialog } from "../components/ui/ConfirmDialog"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/auth.css"

function TwoFactorSettings({ onNavigate }) {
  const { currentUser } = useUser()
  const [enabled, setEnabled] = useState(false)
  const [loading, setLoading] = useState(true)
  const [setupMode, setSetupMode] = useState(false)
  const [qrCodeUrl, setQrCodeUrl] = useState("")
  const [manualEntryKey, setManualEntryKey] = useState("")
  const [secret, setSecret] = useState("")
  const [verificationCode, setVerificationCode] = useState("")
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [verifying, setVerifying] = useState(false)
  const [showDisableConfirm, setShowDisableConfirm] = useState(false)

  useEffect(() => {
    if (currentUser && currentUser.id) {
      loadTwoFactorStatus()
    }
  }, [currentUser])

  const loadTwoFactorStatus = async () => {
    try {
      setLoading(true)
      const status = await twoFactorAPI.getStatus(currentUser.id)
      setEnabled(status.enabled === true)
    } catch (err) {
      console.error("Ошибка загрузки статуса 2FA:", err)
      setError("Не удалось загрузить статус 2FA")
    } finally {
      setLoading(false)
    }
  }

  const handleEnable = async () => {
    try {
      setError("")
      setSuccess("")
      setLoading(true)

      const response = await twoFactorAPI.enable(currentUser.id)
      setQrCodeUrl(response.qrCodeUrl)
      setManualEntryKey(response.manualEntryKey)
      setSecret(response.secret)
      setSetupMode(true)
    } catch (err) {
      console.error("Ошибка включения 2FA:", err)
      setError(err.message || "Не удалось начать настройку 2FA")
    } finally {
      setLoading(false)
    }
  }

  const handleVerify = async () => {
    if (!verificationCode || verificationCode.length !== 6) {
      setError("Введите 6-значный код")
      return
    }

    try {
      setError("")
      setSuccess("")
      setVerifying(true)

      await twoFactorAPI.verifySetup(currentUser.id, verificationCode, secret)
      setSuccess("2FA успешно включена!")
      setEnabled(true)
      setSetupMode(false)
      setVerificationCode("")
      setSecret("")
      
      // Скрываем сообщение об успехе через 3 секунды
      setTimeout(() => setSuccess(""), 3000)
    } catch (err) {
      console.error("Ошибка верификации 2FA:", err)
      setError(err.message || "Неверный код. Попробуйте еще раз.")
    } finally {
      setVerifying(false)
    }
  }

  const handleDisable = () => {
    setShowDisableConfirm(true)
  }

  const confirmDisable = async () => {
    setShowDisableConfirm(false)
    try {
      setError("")
      setSuccess("")
      setLoading(true)

      await twoFactorAPI.disable(currentUser.id)
      setEnabled(false)
      setSuccess("2FA отключена")
      
      setTimeout(() => setSuccess(""), 3000)
    } catch (err) {
      console.error("Ошибка отключения 2FA:", err)
      setError(err.message || "Не удалось отключить 2FA")
    } finally {
      setLoading(false)
    }
  }

  if (loading && !setupMode) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div style={{ textAlign: "center", padding: "2rem" }}>Загрузка...</div>
        </div>
      </div>
    )
  }

  if (setupMode) {
    return (
      <div className="auth-container">
        <div className="auth-card" style={{ maxWidth: "500px" }}>
          <div className="auth-logo">
            <h1>Настройка 2FA</h1>
            <p>Отсканируйте QR-код</p>
          </div>

          {error && <div className="error-message">{error}</div>}
          {success && (
            <div className="success-message" style={{
              padding: "1rem",
              backgroundColor: "#d4edda",
              color: "#155724",
              borderRadius: "4px",
              marginBottom: "1rem"
            }}>
              {success}
            </div>
          )}

          <div style={{ textAlign: "center", marginBottom: "1.5rem" }}>
            <p style={{ marginBottom: "1rem" }}>
              Отсканируйте QR-код с помощью приложения аутентификатора (Google Authenticator, Authy и т.д.)
            </p>
            
            {qrCodeUrl && (
              <div style={{ marginBottom: "1rem" }}>
                <img
                  src={qrCodeUrl}
                  alt="QR Code"
                  style={{
                    width: "250px",
                    height: "250px",
                    border: "1px solid #ddd",
                    borderRadius: "8px",
                    padding: "10px",
                    backgroundColor: "white"
                  }}
                />
              </div>
            )}

            {manualEntryKey && (
              <div style={{ marginBottom: "1rem" }}>
                <p style={{ fontSize: "0.9rem", color: "#666", marginBottom: "0.5rem" }}>
                  Или введите ключ вручную:
                </p>
                <code
                  style={{
                    display: "inline-block",
                    padding: "0.5rem 1rem",
                    backgroundColor: "#f5f5f5",
                    borderRadius: "4px",
                    fontFamily: "monospace",
                    fontSize: "1.1rem",
                    letterSpacing: "0.1rem"
                  }}
                >
                  {manualEntryKey}
                </code>
              </div>
            )}
          </div>

          <div className="input-group">
            <label className="input-label">Код из приложения</label>
            <input
              type="text"
              className="input"
              placeholder="000000"
              value={verificationCode}
              onChange={(e) => {
                setVerificationCode(e.target.value.replace(/\D/g, "").slice(0, 6))
                setError("")
              }}
              maxLength={6}
              style={{ textAlign: "center", letterSpacing: "0.5rem", fontSize: "1.5rem" }}
              disabled={verifying}
            />
          </div>

          <button
            type="button"
            className="btn btn-primary"
            style={{ width: "100%", marginBottom: "0.5rem" }}
            onClick={handleVerify}
            disabled={verifying || verificationCode.length !== 6}
          >
            {verifying ? "Проверка..." : "Подтвердить и включить"}
          </button>

          <button
            type="button"
            className="btn btn-secondary"
            style={{ width: "100%" }}
            onClick={() => {
              setSetupMode(false)
              setError("")
              setVerificationCode("")
              setSecret("")
            }}
            disabled={verifying}
          >
            Отмена
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="auth-container">
      <div className="auth-card" style={{ maxWidth: "500px" }}>
        <div className="auth-logo">
          <h1>Двухфакторная аутентификация</h1>
        </div>

        {error && <div className="error-message">{error}</div>}
        {success && (
          <div className="success-message" style={{
            padding: "1rem",
            backgroundColor: "#d4edda",
            color: "#155724",
            borderRadius: "4px",
            marginBottom: "1rem"
          }}>
            {success}
          </div>
        )}

        <div style={{ padding: "1.5rem" }}>
          <p style={{ marginBottom: "1.5rem", color: "#666" }}>
            Двухфакторная аутентификация (2FA) добавляет дополнительный уровень безопасности вашему аккаунту.
            При включенной 2FA вам потребуется ввести код из приложения аутентификатора при каждом входе.
          </p>

          {enabled ? (
            <div>
              <div
                style={{
                  padding: "1rem",
                  backgroundColor: "#d4edda",
                  color: "#155724",
                  borderRadius: "4px",
                  marginBottom: "1.5rem",
                  textAlign: "center"
                }}
              >
                ✓ 2FA включена
              </div>
              <button
                type="button"
                className="btn btn-secondary"
                style={{ width: "100%" }}
                onClick={handleDisable}
                disabled={loading}
              >
                Отключить 2FA
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="btn btn-primary"
              style={{ width: "100%" }}
              onClick={handleEnable}
              disabled={loading}
            >
              Включить 2FA
            </button>
          )}

          <button
            type="button"
            className="btn btn-secondary"
            style={{ width: "100%", marginTop: "1rem" }}
            onClick={() => onNavigate("settings")}
          >
            Назад к настройкам
          </button>
        </div>

        {/* Confirm Dialog for disabling 2FA */}
        <ConfirmDialog
          open={showDisableConfirm}
          title="Отключить 2FA"
          message="Вы уверены, что хотите отключить двухфакторную аутентификацию? Это снизит безопасность вашего аккаунта."
          confirmText="Отключить"
          cancelText="Отмена"
          confirmVariant="destructive"
          onConfirm={confirmDisable}
          onCancel={() => setShowDisableConfirm(false)}
        />
      </div>
    </div>
  )
}

export default TwoFactorSettings

