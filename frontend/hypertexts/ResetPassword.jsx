"use client"

import { useState, useEffect } from "react"
import { passwordResetAPI } from "../lib/api"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/auth.css"

function ResetPassword({ onNavigate, token: propToken }) {
  const [token, setToken] = useState(propToken || "")
  const [formData, setFormData] = useState({
    newPassword: "",
    confirmPassword: "",
  })
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
      validateToken(tokenValue)
    } else {
      setValidating(false)
      setTokenValid(false)
      setError("Токен сброса пароля не найден")
    }
  }, [propToken])

  const validateToken = async (tokenValue) => {
    try {
      const response = await passwordResetAPI.validateToken(tokenValue)
      setTokenValid(response.valid === true)
      if (!response.valid) {
        setError(response.message || "Токен недействителен или истек")
      }
    } catch (err) {
      console.error("Ошибка проверки токена:", err)
      setTokenValid(false)
      setError("Ошибка при проверке токена")
    } finally {
      setValidating(false)
    }
  }

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
    setError("")
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Валидация
    if (!formData.newPassword || !formData.confirmPassword) {
      setError("Пожалуйста, заполните все поля")
      return
    }

    if (formData.newPassword.length < 8) {
      setError("Пароль должен содержать минимум 8 символов")
      return
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setError("Пароли не совпадают")
      return
    }

    if (!token) {
      setError("Токен сброса пароля не найден")
      return
    }

    setLoading(true)
    setError("")

    try {
      await passwordResetAPI.confirmReset(token, formData.newPassword)
      setSuccess(true)
    } catch (err) {
      console.error("Ошибка сброса пароля:", err)
      setError(err.message || "Ошибка при сбросе пароля. Попробуйте еще раз.")
    } finally {
      setLoading(false)
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

  if (validating) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-logo">
            <h1>CoActivity</h1>
            <p>Проверка токена...</p>
          </div>
          <div style={{ textAlign: "center", padding: "2rem" }}>
            Загрузка...
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
            {error || "Токен недействителен или истек. Запросите новую ссылку для сброса пароля."}
          </div>

          <div className="auth-footer">
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault()
                handleNavigate("forgot-password")
              }}
            >
              Запросить новую ссылку
            </a>
            {" | "}
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

  if (success) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-logo">
            <h1>CoActivity</h1>
            <p>Пароль изменен</p>
          </div>

          <div className="success-message" style={{ 
            padding: "1rem", 
            backgroundColor: "#d4edda", 
            color: "#155724", 
            borderRadius: "4px",
            marginBottom: "1rem"
          }}>
            Пароль успешно изменен! Теперь вы можете войти с новым паролем.
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

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>CoActivity</h1>
          <p>Установка нового пароля</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">Новый пароль *</label>
            <input
              type="password"
              name="newPassword"
              className="input"
              placeholder="Минимум 8 символов"
              value={formData.newPassword}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <div className="input-group">
            <label className="input-label">Подтвердите пароль *</label>
            <input
              type="password"
              name="confirmPassword"
              className="input"
              placeholder="Повторите новый пароль"
              value={formData.confirmPassword}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: "100%" }} disabled={loading}>
            {loading ? "Сохранение..." : "Сохранить новый пароль"}
          </button>
        </form>

        <div className="auth-footer">
          Вспомнили пароль?
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault()
              handleNavigate("login")
            }}
          >
            Войти
          </a>
        </div>
      </div>
    </div>
  )
}

export default ResetPassword
