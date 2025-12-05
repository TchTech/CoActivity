"use client"

import { useState, useEffect } from "react"
import { roomAPI } from "../../lib/api"
import { handleApiError } from "../../types"
import type { RoomJoinRequest, ApproveJoinRequestRequest, RejectJoinRequestRequest } from "../../types"
import { RejectRequestDialog } from "./RejectRequestDialog"
import "../../styles/variables.css"
import "../../styles/global.css"
import "../../styles/components.css"

interface PendingRequestsListProps {
  roomId: number
  userId: number // Admin/creator user ID
  isAdmin: boolean
  isRoomClosed?: boolean
  onRequestApproved?: (requestId: number) => void
  onRequestRejected?: (requestId: number) => void
  onError?: (error: string) => void
}

/**
 * Admin/creator view component for managing pending room join requests.
 * Displays a list of pending requests with approve/reject actions.
 */
export function PendingRequestsList({
  roomId,
  userId,
  isAdmin,
  isRoomClosed = false,
  onRequestApproved,
  onRequestRejected,
  onError,
}: PendingRequestsListProps) {
  const [requests, setRequests] = useState<RoomJoinRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false)
  const [selectedRequest, setSelectedRequest] = useState<RoomJoinRequest | null>(null)
  const [processingRequestId, setProcessingRequestId] = useState<number | null>(null)

  // Load pending requests
  useEffect(() => {
    if (!isAdmin || !roomId) {
      setLoading(false)
      return
    }

    const loadRequests = async () => {
      setLoading(true)
      setError(null)

      try {
        const data = await roomAPI.getPendingRequests(roomId, userId)
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
  }, [roomId, userId, isAdmin, onError])

  const handleApprove = async (request: RoomJoinRequest) => {
    if (isRoomClosed) {
      const errorMsg = "Нельзя одобрить заявку для закрытой комнаты"
      setError(errorMsg)
      if (onError) {
        onError(errorMsg)
      }
      return
    }

    setProcessingRequestId(request.id)
    setError(null)

    try {
      const approveRequest: ApproveJoinRequestRequest = {
        adminId: userId,
      }

      await roomAPI.approveRequest(roomId, request.id, request.userId, approveRequest)
      
      // Remove approved request from list
      setRequests(prev => prev.filter(r => r.id !== request.id))
      
      if (onRequestApproved) {
        onRequestApproved(request.id)
      }
    } catch (err: any) {
      const errorMessage = handleApiError(err)
      setError(errorMessage)
      if (onError) {
        onError(errorMessage)
      }
    } finally {
      setProcessingRequestId(null)
    }
  }

  const handleRejectClick = (request: RoomJoinRequest) => {
    setSelectedRequest(request)
    setRejectDialogOpen(true)
  }

  const handleReject = async (requestId: number, rejectRequest: RejectJoinRequestRequest) => {
    if (!selectedRequest) return

    setProcessingRequestId(requestId)
    setError(null)

    try {
      await roomAPI.rejectRequest(roomId, requestId, selectedRequest.userId, rejectRequest)
      
      // Remove rejected request from list
      setRequests(prev => prev.filter(r => r.id !== requestId))
      
      setRejectDialogOpen(false)
      setSelectedRequest(null)
      
      if (onRequestRejected) {
        onRequestRejected(requestId)
      }
    } catch (err: any) {
      const errorMessage = handleApiError(err)
      setError(errorMessage)
      if (onError) {
        onError(errorMessage)
      }
    } finally {
      setProcessingRequestId(null)
    }
  }

  if (!isAdmin) {
    return null
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
    <>
      <div className="card">
        <div className="card-header">
          <h3 style={{ margin: 0, fontSize: "var(--font-size-lg)", color: "var(--accent-gold)" }}>
            Заявки на вступление
          </h3>
          {isRoomClosed && (
            <span className="badge" style={{ 
              backgroundColor: "var(--error-color)",
              color: "white"
            }}>
              Комната закрыта
            </span>
          )}
        </div>
        <div className="card-body">
          {requests.length === 0 ? (
            <div style={{ 
              padding: "var(--spacing-lg)",
              textAlign: "center",
              color: "var(--text-muted)"
            }}>
              Нет pending заявок
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

              {requests.map((request) => (
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
                    <div>
                      <div style={{ 
                        fontWeight: "600", 
                        marginBottom: "var(--spacing-xs)",
                        color: "var(--text-primary)"
                      }}>
                        {request.userName || `Пользователь #${request.userId}`}
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
                        Подана: {new Date(request.createdAt).toLocaleString("ru-RU")}
                      </div>
                    </div>
                    <span className="badge badge-gold">
                      {request.status}
                    </span>
                  </div>

                  <div style={{ display: "flex", gap: "var(--spacing-sm)", marginTop: "var(--spacing-xs)" }}>
                    <button
                      className="btn btn-primary"
                      onClick={() => handleApprove(request)}
                      disabled={isRoomClosed || processingRequestId === request.id}
                      style={{ fontSize: "var(--font-size-sm)" }}
                    >
                      {processingRequestId === request.id ? "Одобрение..." : "Одобрить"}
                    </button>
                    <button
                      className="btn"
                      onClick={() => handleRejectClick(request)}
                      disabled={processingRequestId === request.id}
                      style={{ 
                        fontSize: "var(--font-size-sm)",
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
                      Отклонить
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <RejectRequestDialog
        open={rejectDialogOpen}
        request={selectedRequest}
        adminId={userId}
        onClose={() => {
          setRejectDialogOpen(false)
          setSelectedRequest(null)
        }}
        onReject={handleReject}
        onError={onError}
      />
    </>
  )
}

