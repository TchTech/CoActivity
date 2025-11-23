"use client"

import { useState } from "react"
import { userAPI } from "../lib/api"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/auth.css"

function Login({ onNavigate }) {
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
      // Проверяем, является ли login email или username
      const isEmail = formData.login.includes("@")
      
      // TODO: В backend нет явного эндпоинта для логина
      // Пока что делаем простую проверку через получение профиля
      // В реальном приложении здесь должен быть эндпоинт /users/login
      // который проверяет credentials и устанавливает session cookie
      
      // Временное решение: пробуем найти пользователя по username или email
      // и проверяем пароль (в реальном приложении это должно быть на backend)
      
      // Для демонстрации просто переходим на главную
      // В production здесь должен быть вызов API для аутентификации
      console.log("Попытка входа:", { login: formData.login, isEmail })
      
      // TODO: Добавить реальный эндпоинт для логина в backend
      // const response = await userAPI.login(formData.login, formData.password)
      
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
