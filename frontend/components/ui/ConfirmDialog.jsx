"use client"

import "../../styles/variables.css"
import "../../styles/global.css"
import "../../styles/components.css"

/**
 * Custom confirmation dialog component in project style
 */
export function ConfirmDialog({ open, title, message, onConfirm, onCancel, confirmText = "Подтвердить", cancelText = "Отмена", confirmVariant = "primary" }) {
  if (!open) return null

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
      onClick={onCancel}
    >
      <div
        className="card"
        style={{
          width: "90%",
          maxWidth: "450px",
          backgroundColor: "var(--bg-secondary)",
          boxShadow: "var(--shadow-lg)",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{
          padding: "var(--spacing-lg)",
          borderBottom: "1px solid var(--border-color)",
        }}>
          <h3 style={{ 
            margin: 0, 
            fontSize: "var(--font-size-lg)",
            color: "var(--accent-gold)",
            marginBottom: "var(--spacing-xs)"
          }}>
            {title || "Подтверждение"}
          </h3>
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
          gap: "var(--spacing-sm)",
        }}>
          <button
            className="btn btn-outline"
            onClick={onCancel}
          >
            {cancelText}
          </button>
          <button
            className={confirmVariant === "destructive" ? "btn" : "btn btn-primary"}
            onClick={onConfirm}
            style={confirmVariant === "destructive" ? {
              backgroundColor: "var(--error-color)",
              color: "white"
            } : {}}
            onMouseEnter={(e) => {
              if (confirmVariant === "destructive") {
                e.currentTarget.style.backgroundColor = "#d32f2f"
              }
            }}
            onMouseLeave={(e) => {
              if (confirmVariant === "destructive") {
                e.currentTarget.style.backgroundColor = "var(--error-color)"
              }
            }}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  )
}

