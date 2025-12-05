"use client"

import { useState } from "react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import type { RoomJoinRequest, RejectJoinRequestRequest } from "@/types"
import { handleApiError } from "@/types"

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

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Отклонить заявку</DialogTitle>
          <DialogDescription>
            {request?.userName 
              ? `Вы уверены, что хотите отклонить заявку от ${request.userName}?`
              : "Вы уверены, что хотите отклонить эту заявку?"}
          </DialogDescription>
        </DialogHeader>

        <div style={{ display: "flex", flexDirection: "column", gap: "var(--spacing-md)" }}>
          <div>
            <Label htmlFor="rejection-reason">
              Причина отклонения (необязательно)
            </Label>
            <Textarea
              id="rejection-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Укажите причину отклонения заявки..."
              maxLength={500}
              rows={4}
              style={{ marginTop: "var(--spacing-xs)" }}
            />
            <div style={{ 
              display: "flex", 
              justifyContent: "space-between",
              marginTop: "var(--spacing-xs)",
              fontSize: "var(--font-size-xs)",
              color: "var(--muted-foreground)"
            }}>
              <span>{remainingChars} символов осталось</span>
              {reason.length > 450 && (
                <span style={{ color: "var(--destructive)" }}>
                  Приближается лимит
                </span>
              )}
            </div>
          </div>

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
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={loading}
          >
            Отмена
          </Button>
          <Button
            variant="destructive"
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? "Отклонение..." : "Отклонить заявку"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

