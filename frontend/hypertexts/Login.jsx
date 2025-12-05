"use client"

import { useState } from "react"
import { userAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import AuthAlert from "../components/AuthAlert"
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
  const [alert, setAlert] = useState({ visible: false, message: "", type: "error" })
  const [loading, setLoading] = useState(false)
  
  // Функция для парсинга ошибок и преобразования в понятные сообщения
  const parseError = (error) => {
    const errorMessage = error?.message || error?.toString() || ""
    const errorStatus = error?.status
    const errorData = error?.data
    
    // Проверяем сообщение об ошибке от бэкенда
    if (errorData?.message) {
      const backendMessage = errorData.message.toLowerCase()
      
      if (backendMessage.includes("invalid") && backendMessage.includes("password")) {
        return "Неверный email/логин или пароль. Проверьте правильность введенных данных."
      }
      if (backendMessage.includes("invalid") && (backendMessage.includes("email") || backendMessage.includes("login"))) {
        return "Неверный email/логин или пароль. Проверьте правильность введенных данных."
      }
      if (backendMessage.includes("not found") || backendMessage.includes("user not found")) {
        return "Пользователь с таким email/логином не найден. Проверьте правильность введенных данных."
      }
      if (backendMessage.includes("unauthorized")) {
        return "Неверный email/логин или пароль. Проверьте правильность введенных данных."
      }
      
      return errorData.message
    }
    
    // Проверяем статус HTTP
    if (errorStatus === 401 || errorStatus === 403) {
      return "Неверный email/логин или пароль. Проверьте правильность введенных данных."
    }
    if (errorStatus === 404) {
      return "Пользователь с таким email/логином не найден. Проверьте правильность введенных данных."
    }
    if (errorStatus === 400) {
      return "Неверный формат данных. Проверьте правильность введенных данных."
    }
    if (errorStatus >= 500) {
      return "Произошла ошибка на сервере. Пожалуйста, попробуйте позже."
    }
    
    // Проверяем текст ошибки
    const lowerMessage = errorMessage.toLowerCase()
    if (lowerMessage.includes("invalid") || lowerMessage.includes("неверный")) {
      return "Неверный email/логин или пароль. Проверьте правильность введенных данных."
    }
    if (lowerMessage.includes("not found") || lowerMessage.includes("не найден")) {
      return "Пользователь с таким email/логином не найден. Проверьте правильность введенных данных."
    }
    
    return errorMessage || "Произошла ошибка при входе. Попробуйте еще раз."
  }

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
      setAlert({ visible: true, message: "Пожалуйста, заполните все поля", type: "error" })
      return
    }

    if (formData.password.length < 8) {
      setAlert({ visible: true, message: "Пароль должен содержать минимум 8 символов", type: "error" })
      return
    }

    setLoading(true)
    setAlert({ visible: false, message: "", type: "error" })

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
      const errorMessage = parseError(err)
      setAlert({ visible: true, message: errorMessage, type: "error" })
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

        <AuthAlert 
          type={alert.type} 
          message={alert.message} 
          visible={alert.visible} 
          onClose={() => setAlert({ visible: false, message: "", type: "error" })} 
        />

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
