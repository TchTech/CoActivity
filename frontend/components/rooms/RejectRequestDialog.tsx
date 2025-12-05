"use client"

import { useState } from "react"
import type { RoomJoinRequest, RejectJoinRequestRequest } from "../../types"
import { handleApiError } from "../../types"
import "../../styles/variables.css"
import "../../styles/global.css"
import "../../styles/components.css"

interface RejectRequestDialogProps {
  open: boolean
  request: RoomJoinRequest | null
  adminId: number
  onClose: () => void
  onReject: (requestId: number, rejectRequest: RejectJoinRequestRequest) => Promise<void>
  onError?: (error: string) => void
}

/**
 * Dialog component for rejecting a room join request with optional reason.
 */
export function RejectRequestDialog({
  open,
  request,
  adminId,
  onClose,
  onReject,
  onError,
}: RejectRequestDialogProps) {
  const [reason, setReason] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async () => {
    if (!request) return

    // Validate reason length (max 500 characters)
    if (reason.length > 500) {
      setError("Причина отклонения не должна превышать 500 символов")
      return
    }

    setLoading(true)
    setError(null)

    try {
      const rejectRequest: RejectJoinRequestRequest = {
        adminId,
        reason: reason.trim() || undefined,
      }

      await onReject(request.id, rejectRequest)
      
      // Reset form and close
      setReason("")
      onClose()
    } catch (err: any) {
      const errorMessage = handleApiError(err)
      setError(errorMessage)
      if (onError) {
        onError(errorMessage)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    if (!loading) {
      setReason("")
      setError(null)
      onClose()
    }
  }

  const remainingChars = 500 - reason.length

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
      onClick={handleClose}
    >
      <div
        className="card"
        style={{
          width: "90%",
          maxWidth: "500px",
          maxHeight: "90vh",
          overflowY: "auto",
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
            Отклонить заявку
          </h3>
          <p style={{ 
            margin: 0,
            fontSize: "var(--font-size-sm)",
            color: "var(--text-secondary)"
          }}>
            {request?.userName 
              ? `Вы уверены, что хотите отклонить заявку от ${request.userName}?`
              : "Вы уверены, что хотите отклонить эту заявку?"}
          </p>
        </div>

        <div style={{ 
          padding: "var(--spacing-lg)",
          display: "flex", 
          flexDirection: "column", 
          gap: "var(--spacing-md)" 
        }}>
          <div>
            <label 
              htmlFor="rejection-reason"
              className="input-label"
            >
              Причина отклонения (необязательно)
            </label>
            <textarea
              id="rejection-reason"
              className="input textarea"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Укажите причину отклонения заявки..."
              maxLength={500}
              rows={4}
            />
            <div style={{ 
              display: "flex", 
              justifyContent: "space-between",
              marginTop: "var(--spacing-xs)",
              fontSize: "var(--font-size-xs)",
              color: "var(--text-muted)"
            }}>
              <span>{remainingChars} символов осталось</span>
              {reason.length > 450 && (
                <span style={{ color: "var(--error-color)" }}>
                  Приближается лимит
                </span>
              )}
            </div>
          </div>

          {error && (
            <div style={{
              padding: "var(--spacing-sm)",
              borderRadius: "var(--radius-md)",
              backgroundColor: "rgba(244, 67, 54, 0.1)",
              color: "var(--error-color)",
              fontSize: "var(--font-size-sm)",
              border: "1px solid rgba(244, 67, 54, 0.3)"
            }}>
              {error}
            </div>
          )}
        </div>

        <div style={{
          padding: "var(--spacing-lg)",
          borderTop: "1px solid var(--border-color)",
          display: "flex",
          justifyContent: "flex-end",
          gap: "var(--spacing-sm)",
        }}>
          <button
            className="btn btn-outline"
            onClick={handleClose}
            disabled={loading}
          >
            Отмена
          </button>
          <button
            className="btn"
            onClick={handleSubmit}
            disabled={loading}
            style={{
              backgroundColor: "var(--error-color)",
              color: "white"
            }}
            onMouseEnter={(e) => {
              if (!e.currentTarget.disabled) {
                e.currentTarget.style.backgroundColor = "#d32f2f"
              }
            }}
            onMouseLeave={(e) => {
              if (!e.currentTarget.disabled) {
                e.currentTarget.style.backgroundColor = "var(--error-color)"
              }
            }}
          >
            {loading ? "Отклонение..." : "Отклонить заявку"}
          </button>
        </div>
      </div>
    </div>
  )
}

