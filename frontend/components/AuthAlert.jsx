"use client"

import { useEffect } from "react"
import "../styles/variables.css"
import "../styles/components.css"

/**
 * Компонент для отображения уведомлений об ошибках/успехе в формах авторизации
 * @param {string} type - Тип alert: 'error', 'warning', 'success', 'info'
 * @param {string} message - Текст сообщения
 * @param {boolean} visible - Видимость alert
 * @param {function} onClose - Функция закрытия alert
 */
function AuthAlert({ type = "error", message, visible, onClose }) {
  useEffect(() => {
    if (visible && onClose) {
      // Автоматически закрываем через 5 секунд для ошибок, 3 секунды для успеха
      const timeout = type === "success" ? 3000 : 5000
      const timer = setTimeout(() => {
        onClose()
      }, timeout)
      return () => clearTimeout(timer)
    }
  }, [visible, type, onClose])

  if (!visible || !message) return null

  const alertConfig = {
    error: {
      bgColor: "rgba(244, 67, 54, 0.15)",
      borderColor: "var(--error-color)",
      textColor: "#ff6b6b",
      icon: "⚠️",
    },
    warning: {
      bgColor: "rgba(255, 152, 0, 0.15)",
      borderColor: "var(--warning-color)",
      textColor: "#ffb74d",
      icon: "⚠️",
    },
    success: {
      bgColor: "rgba(76, 175, 80, 0.15)",
      borderColor: "var(--success-color)",
      textColor: "#81c784",
      icon: "✓",
    },
    info: {
      bgColor: "rgba(33, 150, 243, 0.15)",
      borderColor: "var(--info-color)",
      textColor: "#64b5f6",
      icon: "ℹ️",
    },
  }

  const config = alertConfig[type] || alertConfig.error

  return (
    <div
      style={{
        backgroundColor: config.bgColor,
        border: `1px solid ${config.borderColor}`,
        borderRadius: "var(--radius-md)",
        padding: "var(--spacing-md)",
        marginBottom: "var(--spacing-lg)",
        display: "flex",
        alignItems: "flex-start",
        gap: "var(--spacing-sm)",
        animation: "slideDown 0.3s ease-out",
        position: "relative",
      }}
    >
      <span style={{ fontSize: "18px", lineHeight: "1" }}>{config.icon}</span>
      <div style={{ flex: 1, color: config.textColor, fontSize: "var(--font-size-sm)", lineHeight: "1.5" }}>
        {message}
      </div>
      {onClose && (
        <button
          onClick={onClose}
          style={{
            background: "none",
            border: "none",
            color: config.textColor,
            cursor: "pointer",
            fontSize: "20px",
            lineHeight: "1",
            padding: "0",
            marginLeft: "var(--spacing-xs)",
            opacity: 0.7,
            transition: "opacity 0.2s",
          }}
          onMouseEnter={(e) => (e.currentTarget.style.opacity = "1")}
          onMouseLeave={(e) => (e.currentTarget.style.opacity = "0.7")}
          aria-label="Закрыть"
        >
          ×
        </button>
      )}
      <style jsx>{`
        @keyframes slideDown {
          from {
            opacity: 0;
            transform: translateY(-10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </div>
  )
}

export default AuthAlert

