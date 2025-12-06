"use client"

import { useState, useEffect } from "react"
import { roomAPI } from "../../lib/api"
import { handleApiError, extractCooldownMinutes, isCooldownError } from "../../types"
import type { RoomJoinRequest } from "../../types"
import "../../styles/variables.css"
import "../../styles/global.css"
import "../../styles/components.css"

interface JoinRequestButtonProps {
  roomId: number
  roomJoinType: "open" | "by_application"
  isMember: boolean
  hasPendingRequest: boolean
  pendingRequestId?: number
  pendingRequest?: RoomJoinRequest | null
  userId: number
  onRequestCreated?: (request: RoomJoinRequest) => void
  onRequestCancelled?: () => void
  onError?: (error: string) => void
  onNavigate?: (page: string, id?: number) => void
}

/**
 * Smart button component for joining a room or creating a join request.
 * Handles different room types, request states, and cooldown periods.
 */
export function JoinRequestButton({
  roomId,
  roomJoinType,
  isMember,
  hasPendingRequest,
  pendingRequestId,
  pendingRequest,
  userId,
  onRequestCreated,
  onRequestCancelled,
  onError,
  onNavigate,
}: JoinRequestButtonProps) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [cooldownRemaining, setCooldownRemaining] = useState<number | null>(null)

  // Calculate cooldown timer if request was recently rejected
  useEffect(() => {
    if (pendingRequest?.status === "rejected" && pendingRequest.lastRejectedAt) {
      const rejectedAt = new Date(pendingRequest.lastRejectedAt).getTime()
      const now = Date.now()
      const cooldownMs = 5 * 60 * 1000 // 5 minutes in milliseconds
      const elapsed = now - rejectedAt
      const remaining = Math.max(0, cooldownMs - elapsed)

      if (remaining > 0) {
        const minutes = Math.ceil(remaining / 60000)
        setCooldownRemaining(minutes)
        
        // Update timer every minute
        const interval = setInterval(() => {
          const newElapsed = Date.now() - rejectedAt
          const newRemaining = Math.max(0, cooldownMs - newElapsed)
          if (newRemaining > 0) {
            setCooldownRemaining(Math.ceil(newRemaining / 60000))
          } else {
            setCooldownRemaining(null)
            clearInterval(interval)
          }
        }, 60000)

        return () => clearInterval(interval)
      } else {
        setCooldownRemaining(null)
      }
    } else {
      setCooldownRemaining(null)
    }
  }, [pendingRequest])

  // Handle direct join for "open" rooms
  const handleJoinRoom = async () => {
    if (isMember) return

    setLoading(true)
    setError(null)

    try {
      await roomAPI.joinRoom(roomId, userId)
      // Success - parent component should refresh room data
      if (onRequestCreated) {
        // For open rooms, we don't have a request object, so we'll just notify
        onRequestCreated({} as RoomJoinRequest)
      }
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

  // Handle opening chat
  const handleOpenChat = () => {
    if (onNavigate) {
      onNavigate("chat", roomId)
    }
  }

  // Handle creating a join request for "by_application" rooms
  const handleCreateRequest = async () => {
    if (isMember || hasPendingRequest) return

    setLoading(true)
    setError(null)

    try {
      const request = await roomAPI.createJoinRequest(roomId, userId, {})
      if (onRequestCreated) {
        onRequestCreated(request)
      }
    } catch (err: any) {
      const errorMessage = handleApiError(err)
      setError(errorMessage)
      
      // Check if it's a cooldown error
      if (isCooldownError(err)) {
        const minutes = extractCooldownMinutes(err)
        if (minutes !== null) {
          setCooldownRemaining(minutes)
        }
      }
      
      if (onError) {
        onError(errorMessage)
      }
    } finally {
      setLoading(false)
    }
  }

  // Handle cancelling a pending request
  const handleCancelRequest = async () => {
    if (!pendingRequestId) return

    setLoading(true)
    setError(null)

    try {
      await roomAPI.cancelRequest(roomId, pendingRequestId, userId)
      if (onRequestCancelled) {
        onRequestCancelled()
      }
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

  // If user is already a member, show "Open Chat" button
  if (isMember) {
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-xs)" }}>
        <button
          className="btn btn-primary"
          onClick={handleOpenChat}
        >
          Открыть чат
        </button>
      </div>
    )
  }

  // Handle "open" room type - direct join
  if (roomJoinType === "open") {
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-xs)" }}>
        <button
          className="btn btn-primary"
          onClick={handleJoinRoom}
          disabled={loading || isMember}
        >
          {loading ? "Присоединение..." : "Присоединиться"}
        </button>
        {error && (
          <span style={{ fontSize: "var(--font-size-sm)", color: "var(--error-color)" }}>
            {error}
          </span>
        )}
      </div>
    )
  }

  // Handle "by_application" room type
  const isRejected = pendingRequest?.status === "rejected"
  const isPending = hasPendingRequest || pendingRequest?.status === "pending"
  const canReapply = isRejected && cooldownRemaining === null

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-xs)" }}>
      {isPending ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-sm)" }}>
          <button
            className="btn btn-secondary"
            disabled
          >
            Заявка отправлена
          </button>
          <button
            className="btn btn-outline"
            onClick={handleCancelRequest}
            disabled={loading}
            style={{ fontSize: "var(--font-size-sm)" }}
          >
            {loading ? "Отмена..." : "Отменить заявку"}
          </button>
        </div>
      ) : isRejected && cooldownRemaining !== null ? (
        <button
          className="btn btn-outline"
          disabled
        >
          Подождите {cooldownRemaining} мин. перед повторной заявкой
        </button>
      ) : canReapply ? (
        <button
          className="btn btn-primary"
          onClick={handleCreateRequest}
          disabled={loading}
        >
          {loading ? "Отправка..." : "Подать заявку повторно"}
        </button>
      ) : (
        <button
          className="btn btn-primary"
          onClick={handleCreateRequest}
          disabled={loading}
        >
          {loading ? "Отправка..." : "Подать заявку"}
        </button>
      )}
      
      {error && (
        <span style={{ fontSize: "var(--font-size-sm)", color: "var(--error-color)" }}>
          {error}
        </span>
      )}
      
      {cooldownRemaining !== null && cooldownRemaining > 0 && (
        <span style={{ fontSize: "var(--font-size-xs)", color: "var(--text-muted)" }}>
          Осталось {cooldownRemaining} мин. до возможности повторной заявки
        </span>
      )}
    </div>
  )
}

