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
  
  // Список доступных категорий (те же, что и в CreateRoom)
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
  
  const handleInterestToggle = (categoryValue) => {
    setFormData((prev) => {
      const currentInterests = prev.interests || []
      const isSelected = currentInterests.includes(categoryValue)
      
      if (isSelected) {
        // Удаляем категорию из списка
        return {
          ...prev,
          interests: currentInterests.filter((item) => item !== categoryValue),
        }
      } else {
        // Добавляем категорию в список
        return {
          ...prev,
          interests: [...currentInterests, categoryValue],
        }
      }
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

    // Валидация: минимум 3 категории должны быть выбраны
    if (!formData.interests || formData.interests.length < 3) {
      setAlert({ visible: true, message: "Пожалуйста, выберите минимум 3 категории интересов", type: "error" })
      return
    }

    if (formData.about.length > 500) {
      setAlert({ visible: true, message: "Описание не должно превышать 500 символов", type: "error" })
      return
    }

    setLoading(true)
    setAlert({ visible: false, message: "", type: "error" })

    try {
      // Проверяем, что все обязательные поля заполнены перед отправкой
      if (!formData.nickname || formData.nickname.trim() === "") {
        setAlert({ visible: true, message: "Поле 'Никнейм' обязательно для заполнения", type: "error" })
        setLoading(false)
        return
      }
      
      if (!formData.email || formData.email.trim() === "") {
        setAlert({ visible: true, message: "Поле 'Email' обязательно для заполнения", type: "error" })
        setLoading(false)
        return
      }
      
      if (!formData.password || formData.password.length < 8) {
        setAlert({ visible: true, message: "Пароль должен содержать минимум 8 символов", type: "error" })
        setLoading(false)
        return
      }
      
      console.log("Начинаем регистрацию с данными:", {
        nickname: formData.nickname,
        email: formData.email,
        passwordLength: formData.password?.length,
        interests: formData.interests
      })
      
      // Регистрация пользователя (шаг 1 - создание аккаунта)
      const user = await userAPI.register(formData.nickname, formData.email, formData.password)
      
      console.log("Пользователь зарегистрирован:", user)
      
      if (!user) {
        setAlert({ visible: true, message: "Ошибка: пользователь не был создан", type: "error" })
        setLoading(false)
        return
      }
      
      // Получаем ID пользователя (может быть как id, так и userId)
      const userId = user?.id || user?.userId
      
      console.log("ID пользователя для сохранения интересов:", userId)
      
      // Сохраняем выбранные категории интересов (важно для рекомендаций)
      // Делаем это асинхронно и не блокируем регистрацию при ошибке
      if (userId && formData.interests && formData.interests.length >= 3) {
        try {
          console.log("Сохраняем интересы для пользователя:", userId, formData.interests)
          await userAPI.updateInterests(userId, formData.interests)
          console.log("Интересы пользователя сохранены:", formData.interests)
        } catch (interestError) {
          console.error("Ошибка при сохранении интересов (не критично):", interestError)
          // Не блокируем регистрацию, если не удалось сохранить интересы
          // Пользователь сможет обновить их позже в настройках профиля
        }
      }
      
      // Обновляем профиль пользователя (имя, адрес для города, о себе)
      if (userId) {
        try {
          const fullName = `${formData.firstName} ${formData.lastName}`.trim()
          console.log("Обновляем профиль пользователя:", { name: fullName, address: formData.city, about: formData.about })
          
          // Обновляем имя
          if (fullName) {
            await userAPI.updateName(userId, { name: fullName })
          }
          
          // Обновляем адрес (город)
          if (formData.city) {
            await userAPI.updateAddress(userId, { address: formData.city })
          }
          
          // Обновляем "о себе"
          if (formData.about) {
            await userAPI.updateAbout(userId, { about: formData.about })
          }
          
          console.log("Профиль пользователя обновлен")
        } catch (profileError) {
          console.error("Ошибка при обновлении профиля (не критично):", profileError)
          // Не блокируем регистрацию
        }
      }
      
      // После успешной регистрации автоматически входим в систему
      try {
        // Проверяем, что email и password доступны
        // Используем email из формы или из объекта user
        const loginEmail = formData.email?.trim() || user?.email?.trim()
        const loginPassword = formData.password
        
        console.log("Данные для автоматического входа:", {
          formDataEmail: formData.email,
          userEmail: user?.email,
          loginEmail: loginEmail,
          passwordLength: loginPassword?.length,
          formDataKeys: Object.keys(formData)
        })
        
        if (!loginEmail) {
          console.error("Email не найден для автоматического входа. formData:", formData, "user:", user)
          throw new Error("Email отсутствует для автоматического входа")
        }
        
        if (!loginPassword) {
          console.error("Пароль не найден для автоматического входа")
          throw new Error("Пароль отсутствует для автоматического входа")
        }
        
        console.log("Вызываем userAPI.login с:", { login: loginEmail, passwordLength: loginPassword.length })
        const loginResponse = await userAPI.login(loginEmail, loginPassword)
        console.log("Автоматический вход после регистрации:", loginResponse)
        
        if (loginResponse && loginResponse.token) {
          login(loginResponse)
          // Переход на главную страницу
          onNavigate("home")
        } else {
          // Если автоматический вход не удался, показываем сообщение об успешной регистрации
          setAlert({ 
            visible: true, 
            message: "Регистрация завершена успешно! Теперь вы можете войти в систему.", 
            type: "success" 
          })
          // Предлагаем перейти на страницу входа через 3 секунды
          setTimeout(() => {
            onNavigate("login")
          }, 3000)
        }
      } catch (loginError) {
        console.error("Ошибка автоматического входа после регистрации:", loginError)
        // Регистрация успешна, но автоматический вход не удался
        setAlert({ 
          visible: true, 
          message: "Регистрация завершена успешно! Теперь вы можете войти в систему.", 
          type: "success" 
        })
        // Предлагаем перейти на страницу входа через 3 секунды
        setTimeout(() => {
          onNavigate("login")
        }, 3000)
      }
    } catch (err) {
      console.error("Ошибка регистрации:", err)
      console.error("Детали ошибки:", {
        message: err?.message,
        status: err?.status,
        data: err?.data
      })
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
              <label className="input-label">Категории интересов * (минимум 3)</label>
              
              {/* Список выбранных категорий */}
              {formData.interests && formData.interests.length > 0 && (
                <div style={{
                  marginBottom: "var(--spacing-md)",
                  padding: "var(--spacing-sm) var(--spacing-md)",
                  backgroundColor: "var(--bg-tertiary)",
                  borderRadius: "var(--radius-md)",
                  border: "1px solid var(--accent-gold)",
                }}>
                  <div style={{
                    fontSize: "var(--font-size-xs)",
                    color: "var(--accent-gold)",
                    marginBottom: "var(--spacing-xs)",
                    fontWeight: "500"
                  }}>
                    Выбранные категории:
                  </div>
                  <div style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "var(--spacing-xs)",
                  }}>
                    {formData.interests.map((interestValue) => {
                      const category = availableCategories.find(c => c.value === interestValue)
                      return category ? (
                        <span
                          key={interestValue}
                          style={{
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "var(--spacing-xs)",
                            padding: "4px 8px",
                            backgroundColor: "var(--accent-gold)",
                            color: "var(--bg-primary)",
                            borderRadius: "var(--radius-sm)",
                            fontSize: "var(--font-size-xs)",
                            fontWeight: "500",
                          }}
                        >
                          <span>✓</span>
                          {category.label}
                        </span>
                      ) : null
                    })}
                  </div>
                </div>
              )}
              
              <div style={{ 
                display: "grid", 
                gridTemplateColumns: "repeat(2, 1fr)", 
                gap: "var(--spacing-sm)",
                marginTop: "var(--spacing-xs)"
              }}>
                {availableCategories.map((category) => {
                  const isSelected = formData.interests?.includes(category.value) || false
                  return (
                    <button
                      key={category.value}
                      type="button"
                      onClick={() => handleInterestToggle(category.value)}
                      style={{
                        padding: "var(--spacing-sm) var(--spacing-md)",
                        border: `2px solid ${isSelected ? "var(--accent-gold)" : "var(--border-primary)"}`,
                        borderRadius: "var(--radius-md)",
                        backgroundColor: isSelected ? "var(--accent-gold)" : "transparent",
                        color: isSelected ? "var(--bg-primary)" : "var(--text-secondary)",
                        cursor: "pointer",
                        fontSize: "var(--font-size-sm)",
                        transition: "all 0.2s ease",
                        textAlign: "center",
                        fontWeight: isSelected ? "600" : "400",
                        position: "relative",
                        boxShadow: isSelected ? "0 2px 8px rgba(212, 175, 55, 0.3)" : "none",
                      }}
                      onMouseEnter={(e) => {
                        if (!isSelected) {
                          e.target.style.backgroundColor = "var(--bg-tertiary)"
                          e.target.style.borderColor = "var(--accent-gold)"
                        } else {
                          e.target.style.boxShadow = "0 4px 12px rgba(212, 175, 55, 0.5)"
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (!isSelected) {
                          e.target.style.backgroundColor = "transparent"
                          e.target.style.borderColor = "var(--border-primary)"
                        } else {
                          e.target.style.boxShadow = "0 2px 8px rgba(212, 175, 55, 0.3)"
                        }
                      }}
                    >
                      {isSelected && (
                        <span style={{
                          position: "absolute",
                          top: "4px",
                          right: "4px",
                          fontSize: "12px",
                          color: "var(--bg-primary)",
                        }}>✓</span>
                      )}
                      {category.label}
                    </button>
                  )
                })}
              </div>
              <div style={{ 
                marginTop: "var(--spacing-sm)", 
                fontSize: "var(--font-size-xs)", 
                color: formData.interests && formData.interests.length >= 3 ? "var(--success-color)" : "var(--text-tertiary)",
                fontWeight: formData.interests && formData.interests.length >= 3 ? "500" : "400",
              }}>
                Выбрано: {formData.interests?.length || 0} / 3 (минимум)
                {formData.interests && formData.interests.length >= 3 && (
                  <span style={{ marginLeft: "var(--spacing-xs)", color: "var(--success-color)" }}>✓</span>
                )}
              </div>
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
