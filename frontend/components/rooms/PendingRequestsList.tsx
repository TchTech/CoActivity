"use client"

import { useState, useEffect } from "react"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { roomAPI } from "@/lib/api"
import { handleApiError } from "@/types"
import type { RoomJoinRequest, ApproveJoinRequestRequest, RejectJoinRequestRequest } from "@/types"
import { RejectRequestDialog } from "./RejectRequestDialog"

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
      <Card>
        <CardContent style={{ padding: "var(--spacing-lg)", textAlign: "center" }}>
          Загрузка заявок...
        </CardContent>
      </Card>
    )
  }

  if (error && requests.length === 0) {
    return (
      <Card>
        <CardContent style={{ padding: "var(--spacing-lg)" }}>
          <div style={{ 
            color: "var(--destructive)",
            fontSize: "var(--font-size-sm)"
          }}>
            {error}
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Заявки на вступление</CardTitle>
          {isRoomClosed && (
            <Badge variant="destructive" style={{ marginTop: "var(--spacing-xs)" }}>
              Комната закрыта
            </Badge>
          )}
        </CardHeader>
        <CardContent>
          {requests.length === 0 ? (
            <div style={{ 
              padding: "var(--spacing-lg)",
              textAlign: "center",
              color: "var(--muted-foreground)"
            }}>
              Нет pending заявок
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-md)" }}>
              {error && (
                <div style={{
                  padding: "var(--spacing-sm)",
                  borderRadius: "var(--radius-md)",
                  backgroundColor: "var(--destructive)/10",
                  color: "var(--destructive)",
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
                    display: "flex",
                    flexDirection: "column",
                    gap: "var(--spacing-sm)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                    <div>
                      <div style={{ fontWeight: "600", marginBottom: "var(--spacing-xs)" }}>
                        {request.userName || `Пользователь #${request.userId}`}
                      </div>
                      {request.message && (
                        <div style={{ 
                          fontSize: "var(--font-size-sm)",
                          color: "var(--muted-foreground)",
                          marginTop: "var(--spacing-xs)"
                        }}>
                          {request.message}
                        </div>
                      )}
                      <div style={{ 
                        fontSize: "var(--font-size-xs)",
                        color: "var(--muted-foreground)",
                        marginTop: "var(--spacing-xs)"
                      }}>
                        Подана: {new Date(request.createdAt).toLocaleString("ru-RU")}
                      </div>
                    </div>
                    <Badge variant="secondary">
                      {request.status}
                    </Badge>
                  </div>

                  <div style={{ display: "flex", gap: "var(--spacing-sm)", marginTop: "var(--spacing-xs)" }}>
                    <Button
                      onClick={() => handleApprove(request)}
                      disabled={isRoomClosed || processingRequestId === request.id}
                      variant="default"
                      size="sm"
                    >
                      {processingRequestId === request.id ? "Одобрение..." : "Одобрить"}
                    </Button>
                    <Button
                      onClick={() => handleRejectClick(request)}
                      disabled={processingRequestId === request.id}
                      variant="destructive"
                      size="sm"
                    >
                      Отклонить
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

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

