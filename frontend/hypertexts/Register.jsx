"use client"

import { useState } from "react"
import { userAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import AuthAlert from "../components/AuthAlert"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/auth.css"

function Register({ onNavigate }) {
  const { login } = useUser()
  const [formData, setFormData] = useState({
    email: "",
    nickname: "",
    password: "",
    confirmPassword: "",
    firstName: "",
    lastName: "",
    city: "",
    interests: [],
    about: "",
  })
  const [alert, setAlert] = useState({ visible: false, message: "", type: "error" })
  const [step, setStep] = useState(1)
  const [loading, setLoading] = useState(false)
  
  // Функция для парсинга ошибок и преобразования в понятные сообщения
  const parseError = (error) => {
    const errorMessage = error?.message || error?.toString() || ""
    const errorStatus = error?.status
    const errorData = error?.data
    
    // Проверяем сообщение об ошибке от бэкенда
    if (errorData?.message) {
      const backendMessage = errorData.message.toLowerCase()
      
      if (backendMessage.includes("email") && (backendMessage.includes("already") || backendMessage.includes("exists") || backendMessage.includes("уже"))) {
        return "Эта почта уже используется другим пользователем. Попробуйте войти или используйте другую почту."
      }
      if (backendMessage.includes("username") && (backendMessage.includes("already") || backendMessage.includes("exists") || backendMessage.includes("уже"))) {
        return "Этот никнейм уже занят. Выберите другой никнейм."
      }
      if (backendMessage.includes("nickname") && (backendMessage.includes("already") || backendMessage.includes("exists") || backendMessage.includes("уже"))) {
        return "Этот никнейм уже занят. Выберите другой никнейм."
      }
      if (backendMessage.includes("invalid") && backendMessage.includes("email")) {
        return "Неверный формат email адреса. Проверьте правильность введенных данных."
      }
      if (backendMessage.includes("password") && backendMessage.includes("short")) {
        return "Пароль должен содержать минимум 8 символов."
      }
      
      return errorData.message
    }
    
    // Проверяем статус HTTP
    if (errorStatus === 400) {
      const lowerMessage = errorMessage.toLowerCase()
      if (lowerMessage.includes("email") && (lowerMessage.includes("already") || lowerMessage.includes("exists"))) {
        return "Эта почта уже используется другим пользователем. Попробуйте войти или используйте другую почту."
      }
      if (lowerMessage.includes("username") && (lowerMessage.includes("already") || lowerMessage.includes("exists"))) {
        return "Этот никнейм уже занят. Выберите другой никнейм."
      }
      return "Неверный формат данных. Проверьте правильность введенных данных."
    }
    if (errorStatus === 409) {
      return "Эта почта или никнейм уже используются другим пользователем. Попробуйте войти или используйте другие данные."
    }
    if (errorStatus >= 500) {
      return "Произошла ошибка на сервере. Пожалуйста, попробуйте позже."
    }
    
    // Проверяем текст ошибки
    const lowerMessage = errorMessage.toLowerCase()
    if (lowerMessage.includes("email") && (lowerMessage.includes("already") || lowerMessage.includes("exists") || lowerMessage.includes("уже"))) {
      return "Эта почта уже используется другим пользователем. Попробуйте войти или используйте другую почту."
    }
    if (lowerMessage.includes("username") && (lowerMessage.includes("already") || lowerMessage.includes("exists") || lowerMessage.includes("уже"))) {
      return "Этот никнейм уже занят. Выберите другой никнейм."
    }
    if (lowerMessage.includes("nickname") && (lowerMessage.includes("already") || lowerMessage.includes("exists") || lowerMessage.includes("уже"))) {
      return "Этот никнейм уже занят. Выберите другой никнейм."
    }
    
    return errorMessage || "Произошла ошибка при регистрации. Попробуйте еще раз."
  }

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleSubmitStep1 = (e) => {
    e.preventDefault()

    if (!formData.email || !formData.password || !formData.confirmPassword) {
      setAlert({ visible: true, message: "Пожалуйста, заполните все поля", type: "error" })
      return
    }
    
    // Проверка формата email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(formData.email)) {
      setAlert({ visible: true, message: "Неверный формат email адреса. Проверьте правильность введенных данных.", type: "error" })
      return
    }

    if (formData.password.length < 8) {
      setAlert({ visible: true, message: "Пароль должен содержать минимум 8 символов", type: "error" })
      return
    }

    if (formData.password !== formData.confirmPassword) {
      setAlert({ visible: true, message: "Пароли не совпадают. Пожалуйста, введите одинаковые пароли в оба поля.", type: "error" })
      return
    }

    setAlert({ visible: false, message: "", type: "error" })
    setStep(2)
  }

  const handleSubmitStep2 = async (e) => {
    e.preventDefault()

    if (!formData.firstName || !formData.lastName || !formData.city || !formData.nickname) {
      setAlert({ visible: true, message: "Пожалуйста, заполните все обязательные поля", type: "error" })
      return
    }

    if (formData.about.length > 500) {
      setAlert({ visible: true, message: "Описание не должно превышать 500 символов", type: "error" })
      return
    }

    setLoading(true)
    setAlert({ visible: false, message: "", type: "error" })

    try {
      // Регистрация пользователя (шаг 1 - создание аккаунта)
      const user = await userAPI.register(formData.nickname, formData.email, formData.password)
      
      console.log("Пользователь зарегистрирован:", user)
      
      // Сохраняем пользователя в контекст
      if (user) {
        login(user)
      }
      
      // TODO: После регистрации нужно заполнить профиль (имя, фамилия, город, о себе)
      // Это можно сделать через обновление профиля, если есть такой эндпоинт
      
      // Переход на главную страницу
      onNavigate("home")
    } catch (err) {
      console.error("Ошибка регистрации:", err)
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
          <p>Регистрация {step === 1 ? "(Шаг 1/2)" : "(Шаг 2/2)"}</p>
        </div>

        <AuthAlert 
          type={alert.type} 
          message={alert.message} 
          visible={alert.visible} 
          onClose={() => setAlert({ visible: false, message: "", type: "error" })} 
        />

        {step === 1 ? (
          <form className="auth-form" onSubmit={handleSubmitStep1}>
            <div className="input-group">
              <label className="input-label">Email *</label>
              <input
                type="email"
                name="email"
                className="input"
                placeholder="your@email.com"
                value={formData.email}
                onChange={handleChange}
              />
            </div>

            <div className="input-group">
              <label className="input-label">Никнейм *</label>
              <input
                type="text"
                name="nickname"
                className="input"
                placeholder="Уникальный никнейм"
                value={formData.nickname}
                onChange={handleChange}
              />
            </div>

            <div className="input-group">
              <label className="input-label">Пароль *</label>
              <input
                type="password"
                name="password"
                className="input"
                placeholder="Минимум 8 символов"
                value={formData.password}
                onChange={handleChange}
              />
            </div>

            <div className="input-group">
              <label className="input-label">Подтвердите пароль *</label>
              <input
                type="password"
                name="confirmPassword"
                className="input"
                placeholder="Повторите пароль"
                value={formData.confirmPassword}
                onChange={handleChange}
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: "100%" }}>
              Далее
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={handleSubmitStep2}>
            <div className="input-group">
              <label className="input-label">Имя *</label>
              <input
                type="text"
                name="firstName"
                className="input"
                placeholder="Ваше имя"
                value={formData.firstName}
                onChange={handleChange}
              />
            </div>

            <div className="input-group">
              <label className="input-label">Фамилия *</label>
              <input
                type="text"
                name="lastName"
                className="input"
                placeholder="Ваша фамилия"
                value={formData.lastName}
                onChange={handleChange}
              />
            </div>

            <div className="input-group">
              <label className="input-label">Город *</label>
              <input
                type="text"
                name="city"
                className="input"
                placeholder="Ваш город"
                value={formData.city}
                onChange={handleChange}
              />
            </div>

            <div className="input-group">
              <label className="input-label">О себе (до 500 символов)</label>
              <textarea
                name="about"
                className="input textarea"
                placeholder="Расскажите о себе..."
                value={formData.about}
                onChange={handleChange}
                maxLength="500"
              />
              <div className="char-counter">{formData.about.length}/500</div>
            </div>

            <div style={{ display: "flex", gap: "var(--spacing-md)" }}>
              <button type="button" className="btn btn-secondary" style={{ flex: 1 }} onClick={() => setStep(1)} disabled={loading}>
                Назад
              </button>
              <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={loading}>
                {loading ? "Регистрация..." : "Зарегистрироваться"}
              </button>
            </div>
          </form>
        )}

        <div className="auth-footer">
          Уже есть аккаунт?
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

export default Register
