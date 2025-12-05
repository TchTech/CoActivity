"use client"

import { useState, useEffect } from "react"
import { emailVerificationAPI } from "../lib/api"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/auth.css"

function VerifyEmail({ onNavigate, token: propToken }) {
  const [token, setToken] = useState(propToken || "")
  const [error, setError] = useState("")
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)
  const [validating, setValidating] = useState(true)
  const [tokenValid, setTokenValid] = useState(false)

  useEffect(() => {
    // Получаем токен из разных источников
    let tokenValue = propToken

    if (!tokenValue && typeof window !== "undefined") {
      // Пытаемся получить из URL параметров
      const urlParams = new URLSearchParams(window.location.search)
      tokenValue = urlParams.get("token")
    }

    if (tokenValue) {
      setToken(tokenValue)
      validateAndVerifyToken(tokenValue)
    } else {
      setValidating(false)
      setTokenValid(false)
      setError("Токен подтверждения email не найден")
    }
  }, [propToken])

  const validateAndVerifyToken = async (tokenValue) => {
    try {
      setValidating(true)
      setError("")
      
      // Сначала проверяем токен
      const validationResponse = await emailVerificationAPI.validateToken(tokenValue)
      
      if (validationResponse.valid === true) {
        setTokenValid(true)
        // Если токен валиден, сразу подтверждаем email
        await verifyEmail(tokenValue)
      } else {
        setTokenValid(false)
        setError(validationResponse.message || "Токен недействителен или истек")
      }
    } catch (err) {
      console.error("Ошибка проверки токена:", err)
      setTokenValid(false)
      setError("Ошибка при проверке токена")
    } finally {
      setValidating(false)
    }
  }

  const verifyEmail = async (tokenValue = token) => {
    if (!tokenValue) {
      setError("Токен подтверждения email не найден")
      return
    }

    setLoading(true)
    setError("")

    try {
      await emailVerificationAPI.verify(tokenValue)
      setSuccess(true)
      setTokenValid(true)
    } catch (err) {
      console.error("Ошибка подтверждения email:", err)
      setError(err.message || "Ошибка при подтверждении email. Попробуйте еще раз.")
      setTokenValid(false)
    } finally {
      setLoading(false)
      setValidating(false)
    }
  }

  const handleNavigate = (page) => {
    if (onNavigate) {
      onNavigate(page)
    } else if (typeof window !== "undefined") {
      if (page === "login") {
        window.location.href = "/"
      } else {
        window.location.href = `/${page}`
      }
    }
  }

  if (validating || loading) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-logo">
            <h1>CoActivity</h1>
            <p>{validating ? "Проверка токена..." : "Подтверждение email..."}</p>
          </div>
          <div style={{ textAlign: "center", padding: "2rem" }}>
            Загрузка...
          </div>
        </div>
      </div>
    )
  }

  if (success) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-logo">
            <h1>CoActivity</h1>
            <p>Email подтвержден</p>
          </div>

          <div className="success-message" style={{
            padding: "1rem",
            backgroundColor: "#d4edda",
            color: "#155724",
            borderRadius: "4px",
            marginBottom: "1rem"
          }}>
            Ваш email успешно подтвержден! Теперь вы можете войти в свой аккаунт.
          </div>

          <div className="auth-footer">
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault()
                handleNavigate("login")
              }}
            >
              Войти в аккаунт
            </a>
          </div>
        </div>
      </div>
    )
  }

  if (!tokenValid) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-logo">
            <h1>CoActivity</h1>
            <p>Ошибка</p>
          </div>

          <div className="error-message">
            {error || "Токен недействителен или истек. Запросите новую ссылку для подтверждения email."}
          </div>

          <div className="auth-footer">
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault()
                handleNavigate("login")
              }}
            >
              Вернуться ко входу
            </a>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>CoActivity</h1>
          <p>Подтверждение email</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <div style={{ padding: "1.5rem", textAlign: "center" }}>
          <p style={{ marginBottom: "1.5rem" }}>
            Нажмите на кнопку ниже, чтобы подтвердить ваш email адрес.
          </p>

          <button
            type="button"
            className="btn btn-primary"
            style={{ width: "100%" }}
            onClick={() => verifyEmail(token)}
            disabled={loading}
          >
            {loading ? "Подтверждение..." : "Подтвердить email"}
          </button>
        </div>

        <div className="auth-footer">
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault()
              handleNavigate("login")
            }}
          >
            Вернуться ко входу
          </a>
        </div>
      </div>
    </div>
  )
}

export default VerifyEmail
