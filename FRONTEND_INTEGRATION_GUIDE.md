# Frontend Integration Guide: Room Join Request & Notification System

## Overview
This guide provides TypeScript interfaces, API client methods, and UI component plans for integrating the Room Join Request and Notification systems into the frontend.

**Frontend Stack:** Next.js 16, React 19, TypeScript  
**API Client:** Custom `fetch` wrapper (see `frontend/lib/api.js`)  
**State Management:** React Context (`UserContext`)

---

## Table of Contents
1. [TypeScript Interfaces](#typescript-interfaces)
2. [API Client Methods](#api-client-methods)
3. [UI Component Plan](#ui-component-plan)
4. [Integration Examples](#integration-examples)

---

## TypeScript Interfaces

### Core Models

```typescript
// frontend/types/room-join-request.ts

/**
 * Room Join Request status types
 */
export type RoomJoinRequestStatus = 
  | "pending" 
  | "approved" 
  | "rejected" 
  | "cancelled";

/**
 * Room Join Request response from API
 */
export interface RoomJoinRequest {
  id: number;
  roomId: number;
  roomName?: string;
  userId: number;
  userName?: string;
  status: RoomJoinRequestStatus;
  message?: string;
  createdAt: string; // ISO 8601 timestamp
  respondedAt?: string; // ISO 8601 timestamp
  responderId?: number;
  responderName?: string;
  rejectionReason?: string;
  lastRejectedAt?: string; // ISO 8601 timestamp (for cooldown)
}

/**
 * Request DTO for creating a room join request
 */
export interface CreateRoomJoinRequestRequest {
  message?: string; // Max 500 characters
}

/**
 * Request DTO for approving a join request
 */
export interface ApproveJoinRequestRequest {
  adminId: number;
}

/**
 * Request DTO for rejecting a join request
 */
export interface RejectJoinRequestRequest {
  adminId: number;
  reason?: string; // Max 500 characters, optional
}

/**
 * Request DTO for closing a room
 */
export interface CloseRoomRequest {
  userId: number;
}

/**
 * Room model (enhanced with closure fields)
 */
export interface Room {
  id: number;
  name: string;
  description?: string;
  category?: string;
  joinType: "open" | "by_application";
  maxCollaborators?: number;
  isClosed: boolean;
  closedAt?: string; // ISO 8601 timestamp
  closedBy?: number; // User ID
  meetingTime?: string; // ISO 8601 timestamp
  meetingType?: "online" | "offline";
  location?: string;
  createdAt: string;
  // ... other room fields
}
```

### Notification Interfaces

```typescript
// frontend/types/notification.ts

/**
 * Notification type constants
 */
export type NotificationType =
  | "MEMBERSHIP_REQUEST"
  | "MEMBERSHIP_APPROVED"
  | "MEMBERSHIP_REJECTED"
  | "MEMBERSHIP_CANCELLED"
  | "ROOM_CLOSED"
  | "ROOM_CAPACITY_REACHED"
  | "USER_BANNED"
  | "POST_PINNED"
  | "COMMENT"
  | "MENTION"
  | "EVENT_REMINDER_1H"
  | "EVENT_REMINDER_24H";

/**
 * Standardized notification data structure
 */
export interface NotificationData {
  roomId?: number;
  requestId?: number;
  requesterId?: number;
  responderId?: number;
  reason?: string;
}

/**
 * Notification model from API
 */
export interface Notification {
  id: number;
  userId: number;
  type: NotificationType;
  title: string;
  content: string;
  data?: string; // JSON string of NotificationData
  isRead: boolean;
  createdAt: string; // ISO 8601 timestamp
  deduplicationHash?: string;
}

/**
 * Helper function to parse notification data
 */
export function parseNotificationData(notification: Notification): NotificationData | null {
  if (!notification.data) return null;
  try {
    return JSON.parse(notification.data) as NotificationData;
  } catch {
    return null;
  }
}
```

---

## API Client Methods

### Room Join Request API

Add these methods to `frontend/lib/api.js`:

```javascript
// Add to roomAPI object in frontend/lib/api.js

export const roomAPI = {
  // ... existing methods ...

  /**
   * Create a new room join request
   * POST /api/rooms/{roomId}/requests?userId={userId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} userId - The user ID requesting to join
   * @param {CreateRoomJoinRequestRequest} request - Optional request with message
   * @returns {Promise<RoomJoinRequest>}
   */
  async createJoinRequest(roomId, userId, request = {}) {
    return request(`/api/rooms/${roomId}/requests`, {
      method: "POST",
      params: { userId },
      body: request,
    })
  },

  /**
   * Get all pending requests for a room (admin/creator view)
   * GET /api/rooms/{roomId}/requests?userId={userId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} userId - The admin/creator user ID
   * @returns {Promise<RoomJoinRequest[]>}
   */
  async getPendingRequests(roomId, userId) {
    return request(`/api/rooms/${roomId}/requests`, {
      method: "GET",
      params: { userId },
    })
  },

  /**
   * Approve a room join request
   * POST /api/rooms/{roomId}/requests/{requestId}/approve?targetUserId={targetUserId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} requestId - The request ID
   * @param {number} targetUserId - The user ID whose request is being approved
   * @param {ApproveJoinRequestRequest} approveRequest - Request with adminId
   * @returns {Promise<void>}
   */
  async approveRequest(roomId, requestId, targetUserId, approveRequest) {
    return request(`/api/rooms/${roomId}/requests/${requestId}/approve`, {
      method: "POST",
      params: { targetUserId },
      body: approveRequest,
    })
  },

  /**
   * Reject a room join request
   * POST /api/rooms/{roomId}/requests/{requestId}/reject?targetUserId={targetUserId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} requestId - The request ID
   * @param {number} targetUserId - The user ID whose request is being rejected
   * @param {RejectJoinRequestRequest} rejectRequest - Request with adminId and optional reason
   * @returns {Promise<void>}
   */
  async rejectRequest(roomId, requestId, targetUserId, rejectRequest) {
    return request(`/api/rooms/${roomId}/requests/${requestId}/reject`, {
      method: "POST",
      params: { targetUserId },
      body: rejectRequest,
    })
  },

  /**
   * Cancel a room join request (user cancels their own request)
   * DELETE /api/rooms/{roomId}/requests/{requestId}?userId={userId}
   * 
   * @param {number} roomId - The room ID
   * @param {number} requestId - The request ID
   * @param {number} userId - The user ID (must be request owner)
   * @returns {Promise<void>}
   */
  async cancelRequest(roomId, requestId, userId) {
    return request(`/api/rooms/${roomId}/requests/${requestId}`, {
      method: "DELETE",
      params: { userId },
    })
  },

  /**
   * Get all pending requests for the current user (applicant view)
   * GET /api/rooms/my-applications?userId={userId}
   * 
   * @param {number} userId - The user ID
   * @returns {Promise<RoomJoinRequest[]>}
   */
  async getMyPendingRequests(userId) {
    return request("/api/rooms/my-applications", {
      method: "GET",
      params: { userId },
    })
  },

  /**
   * Manually close a room
   * POST /api/rooms/{roomId}/close
   * 
   * @param {number} roomId - The room ID
   * @param {CloseRoomRequest} closeRequest - Request with userId
   * @returns {Promise<void>}
   */
  async closeRoom(roomId, closeRequest) {
    return request(`/api/rooms/${roomId}/close`, {
      method: "POST",
      body: closeRequest,
    })
  },

  // Legacy methods (deprecated but maintained for backward compatibility)
  async applyToRoom(roomId, userId) {
    return request(`/api/rooms/${roomId}/apply`, {
      method: "POST",
      body: { userId },
    })
  },

  async createMembershipRequest(roomId, userId, message) {
    return this.createJoinRequest(roomId, userId, message ? { message } : {})
  },

  async getMembershipRequests(roomId, userId) {
    return this.getPendingRequests(roomId, userId)
  },

  async cancelMembershipRequest(roomId, requestId, userId) {
    return this.cancelRequest(roomId, requestId, userId)
  },
}
```

### Error Handling

```typescript
// frontend/types/errors.ts

/**
 * API Error response structure
 */
export interface ApiError {
  message: string;
  status: number;
  data?: Record<string, string>; // For validation errors
}

/**
 * Common error messages from backend
 */
export const ErrorMessages = {
  ROOM_OPEN_TYPE: "Requests can only be created for rooms with joinType 'by_application'",
  ALREADY_MEMBER: "User is already a member of this room",
  PENDING_REQUEST_EXISTS: "You have already applied to this room",
  COOLDOWN_ACTIVE: "You must wait",
  ROOM_AT_CAPACITY: "Room has reached maximum capacity",
  NOT_ADMIN: "Only room creator or administrators can",
  NOT_REQUEST_OWNER: "You can only cancel your own requests",
  REQUEST_NOT_PENDING: "Only pending requests can be",
} as const;
```

---

## UI Component Plan

### 1. Request Management Components

#### `JoinRequestButton.tsx`
**Purpose:** Button to create/cancel a join request  
**Location:** `frontend/components/JoinRequestButton.tsx`

**Props:**
```typescript
interface JoinRequestButtonProps {
  roomId: number;
  roomJoinType: "open" | "by_application";
  isMember: boolean;
  hasPendingRequest: boolean;
  pendingRequestId?: number;
  onRequestCreated?: (request: RoomJoinRequest) => void;
  onRequestCancelled?: () => void;
}
```

**Logic:**
- Show "Join" button for "open" rooms
- Show "Apply to Join" button for "by_application" rooms
- Show "Cancel Request" button if pending request exists
- Show "Cooldown: X minutes" if cooldown is active
- Disable button if user is already a member
- Handle errors (cooldown, already member, etc.)

**State:**
- `loading: boolean`
- `error: string | null`
- `cooldownRemaining: number | null` (minutes)

---

#### `PendingRequestsList.tsx`
**Purpose:** Admin/creator view of pending requests  
**Location:** `frontend/components/PendingRequestsList.tsx`

**Props:**
```typescript
interface PendingRequestsListProps {
  roomId: number;
  isAdmin: boolean; // User is room creator or admin
  onRequestApproved?: (requestId: number) => void;
  onRequestRejected?: (requestId: number) => void;
}
```

**Logic:**
- Fetch pending requests on mount
- Display list with:
  - Applicant name/avatar
  - Request message (if provided)
  - Request creation date
  - Approve/Reject buttons
- Show empty state if no pending requests
- Handle approval/rejection with confirmation dialogs
- Show rejection reason input (optional)
- Refresh list after actions

**State:**
- `requests: RoomJoinRequest[]`
- `loading: boolean`
- `error: string | null`
- `selectedRequest: RoomJoinRequest | null` (for rejection dialog)

---

#### `MyApplicationsList.tsx`
**Purpose:** User's view of their own pending requests  
**Location:** `frontend/components/MyApplicationsList.tsx`

**Props:**
```typescript
interface MyApplicationsListProps {
  userId: number;
  onRequestCancelled?: (requestId: number) => void;
}
```

**Logic:**
- Fetch user's pending requests
- Display list with:
  - Room name
  - Request status (pending)
  - Request creation date
  - Cancel button
- Show status badges (Pending, Approved, Rejected)
- Show rejection reason if rejected
- Show cooldown timer if recently rejected

**State:**
- `requests: RoomJoinRequest[]`
- `loading: boolean`
- `error: string | null`

---

#### `RequestStatusBadge.tsx`
**Purpose:** Visual indicator of request status  
**Location:** `frontend/components/RequestStatusBadge.tsx`

**Props:**
```typescript
interface RequestStatusBadgeProps {
  status: RoomJoinRequestStatus;
  rejectionReason?: string;
}
```

**Logic:**
- Display colored badge based on status:
  - Pending: Yellow/Orange
  - Approved: Green
  - Rejected: Red
  - Cancelled: Gray
- Show tooltip with rejection reason if rejected

---

### 2. Notification Components

#### `NotificationItem.tsx` (Enhanced)
**Purpose:** Display individual notification with action buttons  
**Location:** `frontend/components/NotificationItem.tsx`

**Enhancements:**
- Parse `NotificationData` from `notification.data` field
- Add action buttons based on notification type:
  - `MEMBERSHIP_REQUEST` → "View Request" (navigate to room requests page)
  - `MEMBERSHIP_APPROVED` → "View Room" (navigate to room)
  - `MEMBERSHIP_REJECTED` → "View Room" or "Reapply" (if cooldown passed)
  - `ROOM_CLOSED` → "View Details"
- Show notification timestamp (relative time)
- Handle notification click to mark as read

**Props:**
```typescript
interface NotificationItemProps {
  notification: Notification;
  onMarkAsRead: (id: number) => void;
  onDismiss: (id: number) => void;
  onNavigate: (path: string) => void;
}
```

---

#### `NotificationBadge.tsx`
**Purpose:** Unread notification count badge  
**Location:** `frontend/components/NotificationBadge.tsx`

**Logic:**
- Poll `/api/notifications/{userId}/unread-count` every 30 seconds
- Display count badge (red dot with number)
- Show "99+" if count > 99
- Animate on count change

---

### 3. Room Management Components

#### `RoomClosureButton.tsx`
**Purpose:** Button for room creator/admin to close room  
**Location:** `frontend/components/RoomClosureButton.tsx`

**Props:**
```typescript
interface RoomClosureButtonProps {
  roomId: number;
  isAdmin: boolean;
  isClosed: boolean;
  onRoomClosed?: () => void;
}
```

**Logic:**
- Only show if user is admin/creator
- Show "Close Room" if room is open
- Show "Room Closed" badge if room is closed
- Confirmation dialog before closing
- Show warning about auto-rejecting pending requests

---

#### `RoomCapacityIndicator.tsx`
**Purpose:** Display room capacity and availability  
**Location:** `frontend/components/RoomCapacityIndicator.tsx`

**Props:**
```typescript
interface RoomCapacityIndicatorProps {
  currentCount: number;
  maxCount?: number;
  isClosed: boolean;
}
```

**Logic:**
- Show "X / Y members" if maxCount defined
- Show "X members" if no maxCount
- Color coding:
  - Green: Space available
  - Yellow: Near capacity (80%+)
  - Red: At capacity or closed
- Show "Closed" badge if isClosed

---

### 4. Dialog/Modal Components

#### `RejectRequestDialog.tsx`
**Purpose:** Dialog for rejecting a request with optional reason  
**Location:** `frontend/components/RejectRequestDialog.tsx`

**Props:**
```typescript
interface RejectRequestDialogProps {
  open: boolean;
  request: RoomJoinRequest;
  onClose: () => void;
  onReject: (reason?: string) => Promise<void>;
}
```

**Logic:**
- Text input for rejection reason (max 500 chars)
- Character counter
- "Reject" and "Cancel" buttons
- Validation: reason max 500 characters

---

#### `ApproveRequestDialog.tsx`
**Purpose:** Confirmation dialog for approving a request  
**Location:** `frontend/components/ApproveRequestDialog.tsx`

**Props:**
```typescript
interface ApproveRequestDialogProps {
  open: boolean;
  request: RoomJoinRequest;
  roomCapacity?: { current: number; max: number };
  onClose: () => void;
  onApprove: () => Promise<void>;
}
```

**Logic:**
- Show applicant info
- Show room capacity warning if near limit
- Show warning if approving will auto-reject others
- "Approve" and "Cancel" buttons

---

## Integration Examples

### Example 1: RoomInfo Component Enhancement

```typescript
// frontend/hypertexts/RoomInfo.tsx (enhanced)

import { JoinRequestButton } from "../components/JoinRequestButton"
import { PendingRequestsList } from "../components/PendingRequestsList"
import { RoomClosureButton } from "../components/RoomClosureButton"
import { RoomCapacityIndicator } from "../components/RoomCapacityIndicator"

function RoomInfo({ onNavigate, roomId, currentPage }) {
  const { currentUser } = useUser()
  const [roomData, setRoomData] = useState<Room | null>(null)
  const [pendingRequest, setPendingRequest] = useState<RoomJoinRequest | null>(null)
  const [isAdmin, setIsAdmin] = useState(false)

  // ... existing code ...

  const handleRequestCreated = (request: RoomJoinRequest) => {
    setPendingRequest(request)
    // Show success toast
  }

  const handleRequestCancelled = () => {
    setPendingRequest(null)
    // Show success toast
  }

  const handleRequestApproved = async (requestId: number) => {
    // Refresh room data to show new member
    await loadRoomData()
    // Show success toast
  }

  return (
    <div>
      {/* Room details */}
      
      {/* Capacity indicator */}
      {roomData && (
        <RoomCapacityIndicator
          currentCount={roomData.memberCount || 0}
          maxCount={roomData.maxCollaborators}
          isClosed={roomData.isClosed}
        />
      )}

      {/* Join/Apply button */}
      {!isMember && roomData?.joinType === "by_application" && (
        <JoinRequestButton
          roomId={roomId}
          roomJoinType={roomData.joinType}
          isMember={isMember}
          hasPendingRequest={!!pendingRequest}
          pendingRequestId={pendingRequest?.id}
          onRequestCreated={handleRequestCreated}
          onRequestCancelled={handleRequestCancelled}
        />
      )}

      {/* Admin section */}
      {isAdmin && (
        <>
          <RoomClosureButton
            roomId={roomId}
            isAdmin={isAdmin}
            isClosed={roomData?.isClosed || false}
            onRoomClosed={loadRoomData}
          />
          
          <PendingRequestsList
            roomId={roomId}
            isAdmin={isAdmin}
            onRequestApproved={handleRequestApproved}
            onRequestRejected={loadRoomData}
          />
        </>
      )}
    </div>
  )
}
```

### Example 2: Notifications Panel Enhancement

```typescript
// frontend/components/NotificationsPanel.tsx (enhanced)

import { parseNotificationData } from "../types/notification"

function NotificationsPanel({ userId, onClose, onNavigate }) {
  const [notifications, setNotifications] = useState<Notification[]>([])

  const handleNotificationClick = (notification: Notification) => {
    const data = parseNotificationData(notification)
    
    // Mark as read
    notificationAPI.markAsRead(notification.id, userId)
    
    // Navigate based on type
    switch (notification.type) {
      case "MEMBERSHIP_REQUEST":
        if (data?.roomId) {
          onNavigate(`rooms/${data.roomId}?tab=requests`)
        }
        break
      case "MEMBERSHIP_APPROVED":
      case "MEMBERSHIP_REJECTED":
      case "ROOM_CLOSED":
        if (data?.roomId) {
          onNavigate(`rooms/${data.roomId}`)
        }
        break
      // ... other types
    }
  }

  return (
    <div>
      {notifications.map(notification => (
        <NotificationItem
          key={notification.id}
          notification={notification}
          onMarkAsRead={handleNotificationClick}
          onDismiss={handleDismiss}
          onNavigate={onNavigate}
        />
      ))}
    </div>
  )
}
```

### Example 3: My Applications Page

```typescript
// frontend/hypertexts/MyApplications.tsx (new)

import { MyApplicationsList } from "../components/MyApplicationsList"

function MyApplications({ onNavigate, currentPage }) {
  const { currentUser } = useUser()

  if (!currentUser?.id) {
    return <div>Please log in to view your applications</div>
  }

  return (
    <div>
      <h1>My Applications</h1>
      <MyApplicationsList
        userId={currentUser.id}
        onRequestCancelled={() => {
          // Refresh list or show toast
        }}
      />
    </div>
  )
}
```

---

## State Management Recommendations

### 1. Request State Management

```typescript
// frontend/hooks/useRoomJoinRequest.ts

import { useState, useEffect } from "react"
import { roomAPI } from "../lib/api"
import { RoomJoinRequest } from "../types/room-join-request"

export function useRoomJoinRequest(roomId: number, userId: number) {
  const [pendingRequest, setPendingRequest] = useState<RoomJoinRequest | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!roomId || !userId) return
    
    const loadPendingRequest = async () => {
      try {
        const requests = await roomAPI.getMyPendingRequests(userId)
        const request = requests.find(r => r.roomId === roomId && r.status === "pending")
        setPendingRequest(request || null)
      } catch (err) {
        console.error("Error loading pending request:", err)
      }
    }
    
    loadPendingRequest()
  }, [roomId, userId])

  const createRequest = async (message?: string) => {
    setLoading(true)
    setError(null)
    try {
      const request = await roomAPI.createJoinRequest(roomId, userId, { message })
      setPendingRequest(request)
      return request
    } catch (err: any) {
      setError(err.message || "Failed to create request")
      throw err
    } finally {
      setLoading(false)
    }
  }

  const cancelRequest = async () => {
    if (!pendingRequest) return
    
    setLoading(true)
    setError(null)
    try {
      await roomAPI.cancelRequest(roomId, pendingRequest.id, userId)
      setPendingRequest(null)
    } catch (err: any) {
      setError(err.message || "Failed to cancel request")
      throw err
    } finally {
      setLoading(false)
    }
  }

  return {
    pendingRequest,
    loading,
    error,
    createRequest,
    cancelRequest,
  }
}
```

### 2. Notification State Management

```typescript
// frontend/hooks/useNotifications.ts

import { useState, useEffect, useCallback } from "react"
import { notificationAPI } from "../lib/api"
import { Notification } from "../types/notification"

export function useNotifications(userId: number, pollInterval = 30000) {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)

  const loadNotifications = useCallback(async () => {
    if (!userId) return
    
    try {
      const [all, countData] = await Promise.all([
        notificationAPI.getAll(userId),
        notificationAPI.getUnreadCount(userId),
      ])
      setNotifications(Array.isArray(all) ? all : [])
      setUnreadCount(countData?.count || 0)
    } catch (err) {
      console.error("Error loading notifications:", err)
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    loadNotifications()
    const interval = setInterval(loadNotifications, pollInterval)
    return () => clearInterval(interval)
  }, [loadNotifications, pollInterval])

  const markAsRead = async (notificationId: number) => {
    try {
      await notificationAPI.markAsRead(notificationId, userId)
      setNotifications(prev => 
        prev.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
      )
      setUnreadCount(prev => Math.max(0, prev - 1))
    } catch (err) {
      console.error("Error marking notification as read:", err)
    }
  }

  return {
    notifications,
    unreadCount,
    loading,
    markAsRead,
    refresh: loadNotifications,
  }
}
```

---

## Error Handling Patterns

### API Error Handler

```typescript
// frontend/utils/errorHandler.ts

import { ApiError, ErrorMessages } from "../types/errors"

export function handleApiError(error: any): string {
  if (error.status === 400) {
    // Validation errors
    if (error.data && typeof error.data === "object") {
      const messages = Object.values(error.data).join(", ")
      return messages
    }
    return error.message || "Invalid request"
  }
  
  if (error.status === 403) {
    return "You don't have permission to perform this action"
  }
  
  if (error.status === 404) {
    return "Resource not found"
  }
  
  if (error.status === 409) {
    // Check for specific conflict messages
    if (error.message?.includes("already a member")) {
      return ErrorMessages.ALREADY_MEMBER
    }
    if (error.message?.includes("already applied")) {
      return ErrorMessages.PENDING_REQUEST_EXISTS
    }
    if (error.message?.includes("capacity")) {
      return ErrorMessages.ROOM_AT_CAPACITY
    }
    return error.message || "Conflict occurred"
  }
  
  return error.message || "An error occurred"
}

export function extractCooldownMinutes(error: any): number | null {
  const message = error.message || ""
  const match = message.match(/(\d+)\s+more\s+minute/i)
  return match ? parseInt(match[1]) : null
}
```

---

## Component Implementation Checklist

### Phase 1: Core Components
- [ ] `JoinRequestButton.tsx` - Create/cancel request button
- [ ] `PendingRequestsList.tsx` - Admin view of requests
- [ ] `MyApplicationsList.tsx` - User's own requests
- [ ] `RequestStatusBadge.tsx` - Status indicator

### Phase 2: Notification Components
- [ ] Enhance `NotificationItem.tsx` - Add action buttons
- [ ] Enhance `NotificationsPanel.tsx` - Parse NotificationData
- [ ] `NotificationBadge.tsx` - Unread count badge

### Phase 3: Room Management
- [ ] `RoomClosureButton.tsx` - Close room button
- [ ] `RoomCapacityIndicator.tsx` - Capacity display
- [ ] Enhance `RoomInfo.tsx` - Integrate new components

### Phase 4: Dialogs
- [ ] `RejectRequestDialog.tsx` - Rejection dialog
- [ ] `ApproveRequestDialog.tsx` - Approval confirmation
- [ ] `CooldownDialog.tsx` - Cooldown information

### Phase 5: Pages
- [ ] `MyApplications.tsx` - My applications page
- [ ] Enhance `RoomsList.tsx` - Show request status
- [ ] Enhance `RoomInfo.tsx` - Full integration

---

## API Endpoint Reference

### Room Join Requests

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/rooms/{roomId}/requests?userId={userId}` | Create request |
| GET | `/api/rooms/{roomId}/requests?userId={userId}` | List requests (admin) |
| POST | `/api/rooms/{roomId}/requests/{requestId}/approve?targetUserId={targetUserId}` | Approve request |
| POST | `/api/rooms/{roomId}/requests/{requestId}/reject?targetUserId={targetUserId}` | Reject request |
| DELETE | `/api/rooms/{roomId}/requests/{requestId}?userId={userId}` | Cancel request |
| GET | `/api/rooms/my-applications?userId={userId}` | Get my requests |
| POST | `/api/rooms/{roomId}/close` | Close room |

### Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications/{userId}` | Get all notifications |
| GET | `/api/notifications/{userId}/unread` | Get unread notifications |
| GET | `/api/notifications/{userId}/unread-count` | Get unread count |
| POST | `/api/notifications/{notificationId}/read?userId={userId}` | Mark as read |
| POST | `/api/notifications/{userId}/read-all` | Mark all as read |

---

## Testing Recommendations

### Component Tests
- Test button states (disabled, loading, error)
- Test cooldown display logic
- Test error message parsing
- Test notification data parsing

### Integration Tests
- Test full request flow (create → approve → verify)
- Test error scenarios (cooldown, capacity, permissions)
- Test notification delivery and actions

---

## Next Steps

1. **Create TypeScript type files** in `frontend/types/`
2. **Add API methods** to `frontend/lib/api.js`
3. **Create custom hooks** in `frontend/hooks/`
4. **Build components** following the component plan
5. **Integrate** into existing pages (`RoomInfo`, `RoomsList`, etc.)
6. **Add error handling** and user feedback (toasts, dialogs)
7. **Test** all user flows

---

**Document Version:** 1.0  
**Last Updated:** [Current Date]  
**Status:** ✅ Ready for Frontend Implementation

