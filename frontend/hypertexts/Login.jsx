"use client"

import { useState } from "react"
import { userAPI, twoFactorAPI, emailVerificationAPI } from "../lib/api"
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
    twoFactorCode: "",
  })
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const [requiresTwoFactor, setRequiresTwoFactor] = useState(false)
  const [pendingEmail, setPendingEmail] = useState("")
  const [pendingPassword, setPendingPassword] = useState("")
  const [emailNotVerified, setEmailNotVerified] = useState(false)
  const [resendingEmail, setResendingEmail] = useState(false)
  const [resendSuccess, setResendSuccess] = useState("")

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Если требуется 2FA, обрабатываем код
    if (requiresTwoFactor) {
      if (!formData.twoFactorCode || formData.twoFactorCode.length !== 6) {
        setError("Введите 6-значный код из приложения аутентификатора")
        return
      }

      setLoading(true)
      setError("")

      try {
        const user = await twoFactorAPI.loginWithTwoFactor(
          pendingEmail,
          pendingPassword,
          formData.twoFactorCode
        )
        
        console.log("Успешный вход с 2FA:", user)
        
        if (user && user.token) {
          loginUser(user)
          onNavigate("home")
        }
      } catch (err) {
        console.error("Ошибка входа с 2FA:", err)
        setError(err.message || "Неверный код. Попробуйте еще раз.")
      } finally {
        setLoading(false)
      }
      return
    }

    // Обычный вход
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
      const response = await userAPI.login(formData.login, formData.password)
      
      // Проверяем, требуется ли 2FA
      if (response.requiresTwoFactor === true) {
        setRequiresTwoFactor(true)
        setPendingEmail(formData.login)
        setPendingPassword(formData.password)
        setError("")
        return
      }
      
      console.log("Успешный вход:", response)
      
      // Сохраняем пользователя в контекст
      if (response) {
        loginUser(response)
      }
      
      // Переход на главную страницу
      onNavigate("home")
    } catch (err) {
      console.error("Ошибка входа:", err)
      const errorMessage = err.message || "Неверный логин или пароль"
      
      // Проверяем, является ли ошибка связанной с неподтвержденным email
      if (errorMessage.includes("Email not verified") || errorMessage.includes("не подтвержден")) {
        setEmailNotVerified(true)
        setPendingEmail(formData.login)
        setError("")
      } else {
        setError(errorMessage)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleResendVerificationEmail = async () => {
    if (!pendingEmail) {
      setError("Email не указан")
      return
    }

    setResendingEmail(true)
    setError("")
    setResendSuccess("")

    try {
      await emailVerificationAPI.resend(pendingEmail)
      setResendSuccess("Письмо для подтверждения email отправлено на " + pendingEmail)
      setTimeout(() => {
        setResendSuccess("")
        setEmailNotVerified(false)
      }, 5000)
    } catch (err) {
      console.error("Ошибка повторной отправки письма:", err)
      setError(err.message || "Не удалось отправить письмо. Попробуйте еще раз.")
    } finally {
      setResendingEmail(false)
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
        {resendSuccess && (
          <div className="success-message" style={{
            padding: "1rem",
            backgroundColor: "#d4edda",
            color: "#155724",
            borderRadius: "4px",
            marginBottom: "1rem"
          }}>
            {resendSuccess}
          </div>
        )}

        {emailNotVerified ? (
          <div>
            <div className="error-message" style={{
              padding: "1rem",
              backgroundColor: "#f8d7da",
              color: "#721c24",
              borderRadius: "4px",
              marginBottom: "1rem"
            }}>
              Ваш email не подтвержден. Пожалуйста, проверьте вашу почту и перейдите по ссылке для подтверждения.
            </div>
            <div style={{ padding: "1rem", textAlign: "center" }}>
              <button
                type="button"
                className="btn btn-primary"
                style={{ width: "100%", marginBottom: "0.5rem" }}
                onClick={handleResendVerificationEmail}
                disabled={resendingEmail}
              >
                {resendingEmail ? "Отправка..." : "Отправить письмо повторно"}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                style={{ width: "100%" }}
                onClick={() => {
                  setEmailNotVerified(false)
                  setError("")
                  setResendSuccess("")
                }}
                disabled={resendingEmail}
              >
                Назад
              </button>
            </div>
          </div>
        ) : (
          <>
            <form className="auth-form" onSubmit={handleSubmit}>
              {!requiresTwoFactor ? (
                <>
                  <div className="input-group">
                    <label className="input-label">Логин или Email</label>
                    <input
                      type="text"
                      name="login"
                      className="input"
                      placeholder="Введите никнейм или email"
                      value={formData.login}
                      onChange={handleChange}
                      disabled={loading}
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
                      disabled={loading}
                    />
                  </div>
                </>
              ) : (
                <>
                  <div style={{ marginBottom: "1rem", textAlign: "center" }}>
                    <p>Введите код из приложения аутентификатора</p>
                  </div>
                  <div className="input-group">
                    <label className="input-label">Код 2FA</label>
                    <input
                      type="text"
                      name="twoFactorCode"
                      className="input"
                      placeholder="000000"
                      value={formData.twoFactorCode}
                      onChange={handleChange}
                      maxLength={6}
                      disabled={loading}
                      style={{ textAlign: "center", letterSpacing: "0.5rem", fontSize: "1.5rem" }}
                    />
                  </div>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ width: "100%", marginBottom: "0.5rem" }}
                    onClick={() => {
                      setRequiresTwoFactor(false)
                      setFormData({ ...formData, twoFactorCode: "" })
                      setError("")
                    }}
                    disabled={loading}
                  >
                    Назад
                  </button>
                </>
              )}

              <button type="submit" className="btn btn-primary" style={{ width: "100%" }} disabled={loading}>
                {loading ? "Вход..." : requiresTwoFactor ? "Подтвердить вход" : "Войти"}
              </button>
            </form>

            <div className="auth-footer">
              <div style={{ marginBottom: "0.5rem" }}>
                Нет аккаунта?{" "}
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
              <div>
                <a
                  href="#"
                  onClick={(e) => {
                    e.preventDefault()
                    onNavigate("forgot-password")
                  }}
                >
                  Забыли пароль?
                </a>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default Login
