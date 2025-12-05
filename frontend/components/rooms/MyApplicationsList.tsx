"use client"

import { useState, useEffect } from "react"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { roomAPI } from "@/lib/api"
import { handleApiError } from "@/types"
import type { RoomJoinRequest } from "@/types"

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

  const getStatusBadgeVariant = (status: RoomJoinRequest["status"]) => {
    switch (status) {
      case "pending":
        return "secondary"
      case "approved":
        return "default"
      case "rejected":
        return "destructive"
      case "cancelled":
        return "outline"
      default:
        return "outline"
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
    <Card>
      <CardHeader>
        <CardTitle>Мои заявки</CardTitle>
      </CardHeader>
      <CardContent>
        {requests.length === 0 ? (
          <div style={{ 
            padding: "var(--spacing-lg)",
            textAlign: "center",
            color: "var(--muted-foreground)"
          }}>
            У вас нет активных заявок
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

            {requests.map((request) => {
              const isPending = request.status === "pending"
              const isRejected = request.status === "rejected"
              const canCancel = isPending

              return (
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
                    <div style={{ flex: 1 }}>
                      <div 
                        style={{ 
                          fontWeight: "600", 
                          marginBottom: "var(--spacing-xs)",
                          cursor: onNavigate ? "pointer" : "default"
                        }}
                        onClick={() => onNavigate && onNavigate(`rooms/${request.roomId}`)}
                      >
                        {request.roomName || `Комната #${request.roomId}`}
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
                        Подана: {formatDate(request.createdAt)}
                      </div>

                      {request.respondedAt && (
                        <div style={{ 
                          fontSize: "var(--font-size-xs)",
                          color: "var(--muted-foreground)",
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
                          backgroundColor: "var(--destructive)/10",
                          fontSize: "var(--font-size-sm)",
                          color: "var(--destructive)"
                        }}>
                          <strong>Причина отклонения:</strong> {request.rejectionReason}
                        </div>
                      )}

                      {isRejected && request.lastRejectedAt && (
                        <div style={{ 
                          fontSize: "var(--font-size-xs)",
                          color: "var(--muted-foreground)",
                          marginTop: "var(--spacing-xs)",
                          fontStyle: "italic"
                        }}>
                          Можно подать повторную заявку через 5 минут после отклонения
                        </div>
                      )}
                    </div>

                    <Badge variant={getStatusBadgeVariant(request.status)}>
                      {getStatusLabel(request.status)}
                    </Badge>
                  </div>

                  {canCancel && (
                    <div style={{ marginTop: "var(--spacing-xs)" }}>
                      <Button
                        onClick={() => handleCancel(request)}
                        disabled={cancellingRequestId === request.id}
                        variant="outline"
                        size="sm"
                      >
                        {cancellingRequestId === request.id ? "Отмена..." : "Отменить заявку"}
                      </Button>
                    </div>
                  )}

                  {request.status === "approved" && onNavigate && (
                    <div style={{ marginTop: "var(--spacing-xs)" }}>
                      <Button
                        onClick={() => onNavigate(`rooms/${request.roomId}`)}
                        variant="default"
                        size="sm"
                      >
                        Перейти в комнату
                      </Button>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

