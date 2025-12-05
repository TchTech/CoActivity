"use client"

import { useState } from "react"
import { passwordResetAPI } from "../lib/api"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/auth.css"

function ForgotPassword({ onNavigate }) {
  const [email, setEmail] = useState("")
  const [error, setError] = useState("")
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Валидация
    if (!email) {
      setError("Пожалуйста, введите email")
      return
    }

    if (!email.includes("@")) {
      setError("Пожалуйста, введите корректный email")
      return
    }

    setLoading(true)
    setError("")
    setSuccess(false)

    try {
      await passwordResetAPI.requestReset(email)
      setSuccess(true)
    } catch (err) {
      console.error("Ошибка запроса сброса пароля:", err)
      // Показываем успешное сообщение даже при ошибке для безопасности
      setSuccess(true)
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-logo">
            <h1>CoActivity</h1>
            <p>Проверьте почту</p>
          </div>

          <div className="success-message" style={{ 
            padding: "1rem", 
            backgroundColor: "#d4edda", 
            color: "#155724", 
            borderRadius: "4px",
            marginBottom: "1rem"
          }}>
            Если указанный email зарегистрирован в системе, на него будет отправлено письмо с инструкциями по сбросу пароля.
          </div>

          <div className="auth-footer">
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault()
                onNavigate("login")
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
          <p>Сброс пароля</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">Email</label>
            <input
              type="email"
              name="email"
              className="input"
              placeholder="Введите ваш email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: "100%" }} disabled={loading}>
            {loading ? "Отправка..." : "Отправить инструкции"}
          </button>
        </form>

        <div className="auth-footer">
          Вспомнили пароль?
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault()
              onNavigate("login")
            }}
          >
            Войти
          </a>
        </div>
      </div>
    </div>
  )
}

export default ForgotPassword

