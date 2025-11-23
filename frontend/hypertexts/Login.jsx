"use client"

import { useState } from "react"
import { userAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/auth.css"

function Login({ onNavigate }) {
  const { login: loginUser } = useUser()
  const [formData, setFormData] = useState({
    login: "",
    password: "",
  })
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Валидация
    if (!formData.login || !formData.password) {
      setError("Пожалуйста, заполните все поля")
      return
    }

    if (formData.password.length < 8) {
      setError("Пароль должен содержать минимум 8 символов")
      return
    }

    setLoading(true)
    setError("")

    try {
      // Вызов API для аутентификации
      const user = await userAPI.login(formData.login, formData.password)
      
      console.log("Успешный вход:", user)
      
      // Сохраняем пользователя в контекст
      if (user) {
        loginUser(user)
      }
      
      // Переход на главную страницу
      onNavigate("home")
    } catch (err) {
      console.error("Ошибка входа:", err)
      setError(err.message || "Неверный логин или пароль")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>CoActivity</h1>
          <p>Вход в аккаунт</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">Логин или Email</label>
            <input
              type="text"
              name="login"
              className="input"
              placeholder="Введите никнейм или email"
              value={formData.login}
              onChange={handleChange}
            />
          </div>

          <div className="input-group">
            <label className="input-label">Пароль</label>
            <input
              type="password"
              name="password"
              className="input"
              placeholder="Минимум 8 символов"
              value={formData.password}
              onChange={handleChange}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: "100%" }} disabled={loading}>
            {loading ? "Вход..." : "Войти"}
          </button>
        </form>

        <div className="auth-footer">
          Нет аккаунта?
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault()
              onNavigate("register")
            }}
          >
            Зарегистрироваться
          </a>
        </div>
      </div>
    </div>
  )
}

export default Login
