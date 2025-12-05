"use client"

import { useState, useEffect } from "react"
import { roomAPI } from "../../lib/api"
import { handleApiError } from "../../types"
import type { RoomJoinRequest } from "../../types"
import "../../styles/variables.css"
import "../../styles/global.css"
import "../../styles/components.css"

interface MyApplicationsListProps {
  userId: number
  onRequestCancelled?: (requestId: number) => void
  onError?: (error: string) => void
  onNavigate?: (path: string) => void
}

/**
 * User's view of their own room join requests.
 * Displays all requests (pending, approved, rejected) with status badges.
 */
export function MyApplicationsList({
  userId,
  onRequestCancelled,
  onError,
  onNavigate,
}: MyApplicationsListProps) {
  const [requests, setRequests] = useState<RoomJoinRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cancellingRequestId, setCancellingRequestId] = useState<number | null>(null)

  // Load user's requests
  useEffect(() => {
    if (!userId) {
      setLoading(false)
      return
    }

    const loadRequests = async () => {
      setLoading(true)
      setError(null)

      try {
        const data = await roomAPI.getMyPendingRequests(userId)
        setRequests(Array.isArray(data) ? data : [])
      } catch (err: any) {
        const errorMessage = handleApiError(err)
        setError(errorMessage)
        setRequests([])
        if (onError) {
          onError(errorMessage)
        }
      } finally {
        setLoading(false)
      }
    }

    loadRequests()
  }, [userId, onError])

  const handleCancel = async (request: RoomJoinRequest) => {
    if (request.status !== "pending") {
      return
    }

    setCancellingRequestId(request.id)
    setError(null)

    try {
      await roomAPI.cancelRequest(request.roomId, request.id, userId)
      
      // Remove cancelled request from list
      setRequests(prev => prev.filter(r => r.id !== request.id))
      
      if (onRequestCancelled) {
        onRequestCancelled(request.id)
      }
    } catch (err: any) {
      const errorMessage = handleApiError(err)
      setError(errorMessage)
      if (onError) {
        onError(errorMessage)
      }
    } finally {
      setCancellingRequestId(null)
    }
  }

  const getStatusBadgeStyle = (status: RoomJoinRequest["status"]) => {
    switch (status) {
      case "pending":
        return { backgroundColor: "rgba(212, 175, 55, 0.2)", color: "var(--accent-gold)" }
      case "approved":
        return { backgroundColor: "var(--success-color)", color: "white" }
      case "rejected":
        return { backgroundColor: "var(--error-color)", color: "white" }
      case "cancelled":
        return { backgroundColor: "var(--bg-tertiary)", color: "var(--text-secondary)" }
      default:
        return { backgroundColor: "var(--bg-tertiary)", color: "var(--text-secondary)" }
    }
  }

  const getStatusLabel = (status: RoomJoinRequest["status"]) => {
    switch (status) {
      case "pending":
        return "Ожидает"
      case "approved":
        return "Одобрена"
      case "rejected":
        return "Отклонена"
      case "cancelled":
        return "Отменена"
      default:
        return status
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("ru-RU", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  if (loading) {
    return (
      <div className="card">
        <div style={{ padding: "var(--spacing-lg)", textAlign: "center", color: "var(--text-muted)" }}>
          Загрузка заявок...
        </div>
      </div>
    )
  }

  if (error && requests.length === 0) {
    return (
      <div className="card">
        <div style={{ padding: "var(--spacing-lg)" }}>
          <div style={{ 
            color: "var(--error-color)",
            fontSize: "var(--font-size-sm)"
          }}>
            {error}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="card">
      <div className="card-header">
        <h3 style={{ margin: 0, fontSize: "var(--font-size-lg)", color: "var(--accent-gold)" }}>
          Мои заявки
        </h3>
      </div>
      <div className="card-body">
        {requests.length === 0 ? (
          <div style={{ 
            padding: "var(--spacing-lg)",
            textAlign: "center",
            color: "var(--text-muted)"
          }}>
            У вас нет активных заявок
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-md)" }}>
            {error && (
              <div style={{
                padding: "var(--spacing-sm)",
                borderRadius: "var(--radius-md)",
                backgroundColor: "rgba(244, 67, 54, 0.1)",
                color: "var(--error-color)",
                fontSize: "var(--font-size-sm)"
              }}>
                {error}
              </div>
            )}

            {requests.map((request) => {
              const isPending = request.status === "pending"
              const isRejected = request.status === "rejected"
              const canCancel = isPending
              const badgeStyle = getStatusBadgeStyle(request.status)

              return (
                <div
                  key={request.id}
                  style={{
                    padding: "var(--spacing-md)",
                    borderRadius: "var(--radius-md)",
                    border: "1px solid var(--border-color)",
                    backgroundColor: "var(--bg-secondary)",
                    display: "flex",
                    flexDirection: "column",
                    gap: "var(--spacing-sm)",
                    transition: "all 0.2s ease",
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = "var(--accent-gold)"
                    e.currentTarget.style.backgroundColor = "var(--bg-tertiary)"
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor = "var(--border-color)"
                    e.currentTarget.style.backgroundColor = "var(--bg-secondary)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                    <div style={{ flex: 1 }}>
                      <div 
                        style={{ 
                          fontWeight: "600", 
                          marginBottom: "var(--spacing-xs)",
                          cursor: onNavigate ? "pointer" : "default",
                          color: "var(--text-primary)"
                        }}
                        onClick={() => onNavigate && onNavigate(`roomInfo`, request.roomId)}
                        onMouseEnter={(e) => {
                          if (onNavigate) {
                            e.currentTarget.style.color = "var(--accent-gold)"
                          }
                        }}
                        onMouseLeave={(e) => {
                          if (onNavigate) {
                            e.currentTarget.style.color = "var(--text-primary)"
                          }
                        }}
                      >
                        {request.roomName || `Комната #${request.roomId}`}
                      </div>
                      
                      {request.message && (
                        <div style={{ 
                          fontSize: "var(--font-size-sm)",
                          color: "var(--text-secondary)",
                          marginTop: "var(--spacing-xs)",
                          lineHeight: "1.5"
                        }}>
                          {request.message}
                        </div>
                      )}

                      <div style={{ 
                        fontSize: "var(--font-size-xs)",
                        color: "var(--text-muted)",
                        marginTop: "var(--spacing-xs)"
                      }}>
                        Подана: {formatDate(request.createdAt)}
                      </div>

                      {request.respondedAt && (
                        <div style={{ 
                          fontSize: "var(--font-size-xs)",
                          color: "var(--text-muted)",
                          marginTop: "var(--spacing-xs)"
                        }}>
                          {request.status === "approved" ? "Одобрена" : "Отклонена"}: {formatDate(request.respondedAt)}
                          {request.responderName && ` (${request.responderName})`}
                        </div>
                      )}

                      {isRejected && request.rejectionReason && (
                        <div style={{
                          marginTop: "var(--spacing-sm)",
                          padding: "var(--spacing-sm)",
                          borderRadius: "var(--radius-md)",
                          backgroundColor: "rgba(244, 67, 54, 0.1)",
                          fontSize: "var(--font-size-sm)",
                          color: "var(--error-color)",
                          border: "1px solid rgba(244, 67, 54, 0.3)"
                        }}>
                          <strong>Причина отклонения:</strong> {request.rejectionReason}
                        </div>
                      )}

                      {isRejected && request.lastRejectedAt && (
                        <div style={{ 
                          fontSize: "var(--font-size-xs)",
                          color: "var(--text-muted)",
                          marginTop: "var(--spacing-xs)",
                          fontStyle: "italic"
                        }}>
                          Можно подать повторную заявку через 5 минут после отклонения
                        </div>
                      )}
                    </div>

                    <span className="badge" style={badgeStyle}>
                      {getStatusLabel(request.status)}
                    </span>
                  </div>

                  {canCancel && (
                    <div style={{ marginTop: "var(--spacing-xs)" }}>
                      <button
                        className="btn btn-outline"
                        onClick={() => handleCancel(request)}
                        disabled={cancellingRequestId === request.id}
                        style={{ fontSize: "var(--font-size-sm)" }}
                      >
                        {cancellingRequestId === request.id ? "Отмена..." : "Отменить заявку"}
                      </button>
                    </div>
                  )}

                  {request.status === "approved" && onNavigate && (
                    <div style={{ marginTop: "var(--spacing-xs)" }}>
                      <button
                        className="btn btn-primary"
                        onClick={() => onNavigate(`roomInfo`, request.roomId)}
                        style={{ fontSize: "var(--font-size-sm)" }}
                      >
                        Перейти в комнату
                      </button>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

