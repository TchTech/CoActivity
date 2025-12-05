"use client"

import "../../styles/variables.css"
import "../../styles/global.css"
import "../../styles/components.css"

/**
 * Custom alert dialog component in project style
 */
export function AlertDialog({ open, title, message, onClose, buttonText = "OK", variant = "info" }) {
  if (!open) return null

  const getVariantStyles = () => {
    switch (variant) {
      case "error":
        return {
          titleColor: "var(--error-color)",
          borderColor: "var(--error-color)",
          icon: "⚠️"
        }
      case "success":
        return {
          titleColor: "var(--success-color)",
          borderColor: "var(--success-color)",
          icon: "✓"
        }
      case "warning":
        return {
          titleColor: "var(--warning-color)",
          borderColor: "var(--warning-color)",
          icon: "⚠"
        }
      default:
        return {
          titleColor: "var(--accent-gold)",
          borderColor: "var(--accent-gold)",
          icon: "ℹ️"
        }
    }
  }

  const variantStyles = getVariantStyles()

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.7)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000,
      }}
      onClick={onClose}
    >
      <div
        className="card"
        style={{
          width: "90%",
          maxWidth: "400px",
          backgroundColor: "var(--bg-secondary)",
          boxShadow: "var(--shadow-lg)",
          border: `1px solid ${variantStyles.borderColor}`,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{
          padding: "var(--spacing-lg)",
          borderBottom: "1px solid var(--border-color)",
        }}>
          <div style={{
            display: "flex",
            alignItems: "center",
            gap: "var(--spacing-sm)",
            marginBottom: "var(--spacing-xs)"
          }}>
            <span style={{ fontSize: "var(--font-size-xl)" }}>{variantStyles.icon}</span>
            <h3 style={{ 
              margin: 0, 
              fontSize: "var(--font-size-lg)",
              color: variantStyles.titleColor,
            }}>
              {title || "Уведомление"}
            </h3>
          </div>
          {message && (
            <p style={{ 
              margin: 0,
              marginTop: "var(--spacing-sm)",
              fontSize: "var(--font-size-base)",
              color: "var(--text-secondary)",
              lineHeight: "1.5"
            }}>
              {message}
            </p>
          )}
        </div>

        <div style={{
          padding: "var(--spacing-lg)",
          display: "flex",
          justifyContent: "flex-end",
        }}>
          <button
            className="btn btn-primary"
            onClick={onClose}
          >
            {buttonText}
          </button>
        </div>
      </div>
    </div>
  )
}

