# Implementation Plan: Enhanced Room Join Request & Notification System

## Overview
This document outlines the implementation plan for enhancing the Room Join Request System and Notification System based on the requirements gathered.

**Status:** Ready for Implementation  
**Version:** 1.0  
**Last Updated:** Based on requirements clarification session

---

## Table of Contents
1. [Database Schema](#database-schema)
2. [State Machine Definition](#state-machine-definition)
3. [Business Logic Rules](#business-logic-rules)
4. [Implementation Phases](#implementation-phases)
5. [Component Architecture](#component-architecture)
6. [API Endpoints](#api-endpoints)
7. [Testing Strategy](#testing-strategy)

---

## Database Schema

### Migration File
- **File:** `src/main/resources/sql/migration_v4.sql`
- **Status:** ✅ Created
- **Dependencies:** Must run after `migration_v3.sql`

### Key Schema Changes

#### 1. Enhanced `room_join_requests` Table
- Added `rejection_reason` (TEXT) - Optional reason for rejection
- Added `last_rejected_at` (TIMESTAMP) - For 5-minute cooldown tracking
- Added unique constraint on `(roomId, userId)` for pending requests
- Added indexes for performance

#### 2. Enhanced `rooms` Table
- Added `is_closed` (BOOLEAN) - Room closure status
- Added `closed_at` (TIMESTAMP) - When room was closed
- Added `closed_by` (INTEGER) - Who closed it (if manual)
- Added trigger for auto-closing when `meetingTime` passes

#### 3. Enhanced `notifications` Table
- Added `deduplication_hash` (VARCHAR(64)) - Prevents duplicates
- Added check constraint for valid notification types
- Added index for retention cleanup (10 days)

#### 4. New `notification_deduplication_log` Table
- Tracks recent notifications to prevent duplicates
- Auto-cleans entries older than 1 hour

#### 5. New `room_join_request_history` Table
- Audit trail for all request state changes
- Useful for debugging and compliance

#### 6. Enhanced `user_settings` Table
- Added `membership_request_notifications` (BOOLEAN)
- Added `membership_decision_notifications` (BOOLEAN)

#### 7. Enhanced `room_notification_settings` Table
- Added `membership_request_notifications` (BOOLEAN)

---

## State Machine Definition

### Room Join Request States

```
┌─────────┐
│ PENDING │ ──── (User submits request)
└────┬────┘
     │
     ├─────────────────────────────────────┐
     │                                     │
     ▼                                     ▼
┌──────────┐                        ┌──────────┐
│ APPROVED │                        │ REJECTED │
└──────────┘                        └────┬─────┘
     │                                   │
     │ (User becomes collaborator)      │ (5-min cooldown)
     │                                   │
     └───────────────────────────────────┘
                    │
                    ▼
            ┌──────────────┐
            │  CANCELLED   │ (User cancels)
            └──────────────┘
```

### State Transitions

| From State | To State | Trigger | Who Can Trigger |
|------------|----------|---------|----------------|
| PENDING | APPROVED | Admin/Creator approves | Room admins or creator |
| PENDING | REJECTED | Admin/Creator rejects | Room admins or creator |
| PENDING | CANCELLED | User cancels | Request owner only |
| PENDING | REJECTED | Room closes | System (auto) |
| PENDING | REJECTED | Room deleted | System (auto) |
| PENDING | REJECTED | Room reaches capacity | System (auto) |
| PENDING | REJECTED | User banned | System (auto) |
| REJECTED | PENDING | User reapplies (after 5 min) | Request owner |
| CANCELLED | PENDING | User reapplies | Request owner |

### Room States

| State | Description | Can Accept Requests? |
|-------|-------------|---------------------|
| OPEN | Room is active | ✅ Yes (if joinType = "by_application") |
| CLOSED | Room is closed | ❌ No |

**Room Closure Triggers:**
- Event date (`meetingTime`) has passed → Auto-close
- Room owner manually closes → Manual close
- Room is deleted → Auto-close (cascade)

---

## Business Logic Rules

### Request Creation Rules

1. **Room Type Validation**
   - ✅ Request can ONLY be created for rooms with `joinType = "by_application"`
   - ❌ Requests for `joinType = "open"` must be rejected with error

2. **Room Status Validation**
   - ✅ Room must not be closed (`is_closed = FALSE`)
   - ✅ Room must exist and not be deleted
   - ❌ If room is closed, request creation is allowed but will be pending (admins can manually approve)

3. **User Eligibility**
   - ✅ User must not already be a collaborator
   - ✅ User must not have a pending request
   - ✅ If user was recently rejected, must wait 5 minutes (cooldown)

4. **Cooldown Logic**
   - Check `last_rejected_at` for this user + room combination
   - If `last_rejected_at` exists and is less than 5 minutes ago → Reject with cooldown message
   - If `last_rejected_at` is NULL or > 5 minutes ago → Allow request

5. **Duplicate Prevention**
   - Unique constraint on `(roomId, userId)` for pending requests
   - Check for existing pending request before creating new one

### Request Approval Rules

1. **Permission Check**
   - ✅ User must be room creator OR room admin
   - ✅ Request must be in PENDING state

2. **Capacity Check (Race Condition Prevention)**
   - Use database-level locking (SELECT FOR UPDATE)
   - Check current collaborator count
   - Check `maxCollaborators` limit
   - If at capacity → Reject with "Room at capacity" error
   - If not at capacity → Add user and approve request

3. **State Validation**
   - ✅ User must not already be a collaborator (double-check)
   - ✅ Room must still exist and not be deleted

4. **Auto-Reject Pending Requests**
   - If approving a request causes room to reach capacity:
     - ✅ Approve the current request
     - ✅ Auto-reject all other pending requests
     - ✅ Send notifications to all rejected applicants

### Request Rejection Rules

1. **Permission Check**
   - ✅ User must be room creator OR room admin

2. **State Update**
   - Set status to "rejected"
   - Set `respondedAt` to current timestamp
   - Set `responder` to admin/creator who rejected
   - Set `last_rejected_at` to current timestamp (for cooldown)
   - Set `rejection_reason` if provided

3. **Notification**
   - Send notification to applicant (if notifications enabled)

### Request Cancellation Rules

1. **Permission Check**
   - ✅ Only the request owner (user who created it) can cancel
   - ✅ Request must be in PENDING state

2. **State Update**
   - Set status to "cancelled"
   - No notification sent (per requirements)

### Room Closure Rules

1. **Auto-Closure**
   - Trigger: `meetingTime < NOW()` and room is not already closed
   - Action:
     - Set `is_closed = TRUE`
     - Set `closed_at = meetingTime`
     - Auto-reject all pending requests
     - Send notifications to all applicants

2. **Manual Closure**
   - Trigger: Room owner/admin manually closes room
   - Action:
     - Set `is_closed = TRUE`
     - Set `closed_at = NOW()`
     - Set `closed_by = userId`
     - Auto-reject all pending requests
     - Send notifications to all applicants

3. **Room Deletion**
   - Trigger: Room is deleted
   - Action (via CASCADE):
     - All pending requests are deleted
     - Send notifications to all applicants (before deletion)

### Notification Rules

1. **Notification Types**
   - `MEMBERSHIP_REQUEST` - New request created (to room creator + admins)
   - `MEMBERSHIP_APPROVED` - Request approved (to applicant)
   - `MEMBERSHIP_REJECTED` - Request rejected (to applicant)
   - `MEMBERSHIP_CANCELLED` - Request cancelled by user (NO notification per requirements)
   - `ROOM_CLOSED` - Room closed, request auto-rejected (to applicant)
   - `ROOM_CAPACITY_REACHED` - Room at capacity, request auto-rejected (to applicant)
   - `USER_BANNED` - User banned, request auto-rejected (to applicant)

2. **Notification Recipients**
   - **New Request:** Room creator + All room admins
   - **Request Decision:** Applicant only
   - **Room Closure:** All pending applicants
   - **Capacity Reached:** All remaining pending applicants

3. **Notification Preferences**
   - Check `user_settings.membership_request_notifications` (for room owners/admins)
   - Check `user_settings.membership_decision_notifications` (for applicants)
   - Check `room_notification_settings.membership_request_notifications` (room-level override)
   - If ANY preference is FALSE → Do NOT create notification record

4. **Deduplication**
   - Generate hash: `MD5(userId + type + data)`
   - Check `notification_deduplication_log` for same hash within last 1 hour
   - If exists → Skip notification creation
   - If not exists → Create notification and log hash

5. **Retention Policy**
   - Delete notifications older than 10 days (automated cleanup job)

### Notification Data Schema

Standardized JSON structure for notification `data` field:

```json
{
  "roomId": 123,
  "requestId": 456,
  "requesterId": 789,
  "responderId": 101,
  "reason": "Optional rejection reason"
}
```

**Fields by Notification Type:**

| Type | Required Fields | Optional Fields |
|------|----------------|-----------------|
| MEMBERSHIP_REQUEST | roomId, requestId, requesterId | - |
| MEMBERSHIP_APPROVED | roomId, requestId | responderId |
| MEMBERSHIP_REJECTED | roomId, requestId | responderId, reason |
| ROOM_CLOSED | roomId | - |
| ROOM_CAPACITY_REACHED | roomId | - |
| USER_BANNED | roomId, requestId | - |

---

## Implementation Phases

### Phase 1: Database Migration & Model Updates
**Duration:** 1-2 days  
**Priority:** Critical

#### Tasks:
1. ✅ Create `migration_v4.sql`
2. Run migration on development database
3. Update JPA entities:
   - `RoomJoinRequest.java` - Add new fields
   - `Room.java` - Add closure fields
   - `Notification.java` - Add deduplication_hash
   - `UserSettings.java` - Add notification preferences
   - `RoomNotificationSettings.java` - Add membership request setting
4. Create new entities:
   - `NotificationDeduplicationLog.java`
   - `RoomJoinRequestHistory.java` (optional, for audit)
5. Update repositories with new query methods

#### Deliverables:
- Migration script executed successfully
- All entities updated and tested
- Repository methods added

---

### Phase 2: Core Request Service Logic
**Duration:** 3-4 days  
**Priority:** Critical

#### Tasks:
1. **Request Creation Service**
   - Validate room type (`joinType = "by_application"`)
   - Validate room status (not closed)
   - Check user eligibility (not collaborator, no pending request)
   - Check cooldown (5 minutes after rejection)
   - Create request with proper state
   - Send notifications to creator + admins

2. **Request Approval Service**
   - Permission check (creator or admin)
   - Database locking for race condition prevention
   - Capacity check with atomic update
   - Auto-reject other pending requests if capacity reached
   - Add user to collaborators
   - Update request state
   - Send notification to applicant

3. **Request Rejection Service**
   - Permission check
   - Update state with rejection details
   - Set cooldown timestamp
   - Send notification to applicant

4. **Request Cancellation Service**
   - Permission check (owner only)
   - Update state to cancelled
   - No notification (per requirements)

5. **Room Closure Service**
   - Auto-close when event date passes
   - Manual close by owner/admin
   - Auto-reject all pending requests
   - Send notifications to all applicants

#### Deliverables:
- `RoomJoinRequestService.java` with all business logic
- Unit tests for each service method
- Integration tests for state transitions

---

### Phase 3: Notification Service Enhancement
**Duration:** 2-3 days  
**Priority:** High

#### Tasks:
1. **Notification Creation Service**
   - Check user notification preferences
   - Check room notification settings
   - Generate deduplication hash
   - Check deduplication log
   - Create notification if allowed
   - Log deduplication hash

2. **Notification Recipient Resolution**
   - For new requests: Get creator + all admins
   - For decisions: Get applicant
   - For room closure: Get all pending applicants

3. **Notification Data Builder**
   - Standardized JSON schema builder
   - Type-specific data construction

4. **Notification Cleanup Job**
   - Scheduled job to delete notifications > 10 days old
   - Scheduled job to clean deduplication log > 1 hour old

#### Deliverables:
- Enhanced `NotificationService.java`
- Notification preference checking logic
- Deduplication logic
- Scheduled cleanup jobs

---

### Phase 4: API Endpoints & Controllers
**Duration:** 2-3 days  
**Priority:** High

#### Tasks:
1. Update `RoomController.java`:
   - Enhance existing endpoints
   - Add new endpoints if needed
   - Add proper error handling
   - Add request validation

2. Update `NotificationController.java`:
   - Ensure proper filtering by preferences
   - Add notification settings endpoints

3. Create DTOs:
   - `CreateRoomJoinRequestDTO.java`
   - `ApproveRoomJoinRequestDTO.java`
   - `RejectRoomJoinRequestDTO.java`
   - `CloseRoomDTO.java`
   - `NotificationDataDTO.java` (for standardized schema)

#### Deliverables:
- Updated controllers
- New DTOs
- API documentation (Swagger)

---

### Phase 5: Edge Cases & Error Handling
**Duration:** 2-3 days  
**Priority:** Medium

#### Tasks:
1. **Race Condition Handling**
   - Database locking for approval
   - Transaction management
   - Retry logic for conflicts

2. **Edge Case Handling**
   - User already collaborator but has pending request
   - Multiple admins approving simultaneously
   - Room closure during approval
   - User deletion during pending request
   - Admin removal after approval

3. **Error Messages**
   - User-friendly error messages
   - Proper HTTP status codes
   - Error logging

#### Deliverables:
- Comprehensive error handling
- Edge case tests
- Error message documentation

---

### Phase 6: Testing & Validation
**Duration:** 3-4 days  
**Priority:** Critical

#### Tasks:
1. **Unit Tests**
   - Service layer tests
   - Repository tests
   - DTO validation tests

2. **Integration Tests**
   - Request lifecycle tests
   - Notification delivery tests
   - Room closure tests
   - Cooldown tests

3. **Performance Tests**
   - Concurrent request creation
   - Concurrent approval handling
   - Notification deduplication performance

4. **Manual Testing**
   - Test all user flows
   - Test all edge cases
   - Test notification preferences

#### Deliverables:
- Test coverage > 80%
- All edge cases covered
- Performance benchmarks

---

### Phase 7: Documentation & Deployment
**Duration:** 1-2 days  
**Priority:** Medium

#### Tasks:
1. **API Documentation**
   - Update Swagger/OpenAPI spec
   - Document all endpoints
   - Document error codes

2. **Code Documentation**
   - JavaDoc for all public methods
   - Inline comments for complex logic

3. **Deployment**
   - Run migration on staging
   - Verify data migration
   - Deploy to production

#### Deliverables:
- Complete API documentation
- Code documentation
- Deployment guide

---

## Component Architecture

### Service Layer

```
RoomJoinRequestService
├── createRequest(roomId, userId, message)
├── approveRequest(roomId, requestId, adminId)
├── rejectRequest(roomId, requestId, adminId, reason)
├── cancelRequest(roomId, requestId, userId)
├── getPendingRequests(roomId, userId) // For admins
├── getMyPendingRequests(userId) // For applicants
├── checkCooldown(userId, roomId) // Internal
└── autoRejectPendingRequests(roomId, reason) // Internal

RoomService (Enhanced)
├── closeRoom(roomId, userId) // Manual close
├── checkRoomStatus(roomId) // Internal
└── autoCloseExpiredRooms() // Scheduled job

NotificationService (Enhanced)
├── createNotification(userId, type, title, content, data)
├── createNotificationForMultiple(users, type, title, content, data)
├── checkNotificationPreferences(userId, type, roomId) // Internal
├── generateDeduplicationHash(userId, type, data) // Internal
├── checkDeduplication(hash, userId, type) // Internal
└── cleanupOldNotifications() // Scheduled job
```

### Repository Layer

```
RoomJoinRequestRepository
├── findByRoomIdAndStatus(roomId, status)
├── findByUserIdAndStatus(userId, status)
├── findByRoomIdAndUserIdAndStatus(roomId, userId, status)
├── findRecentRejection(userId, roomId, minutes) // For cooldown
└── findPendingRequestsByRoom(roomId) // For auto-reject

NotificationDeduplicationLogRepository
├── findByHashAndUserAndType(hash, userId, type)
└── deleteOlderThan(timestamp)
```

---

## API Endpoints

### Room Join Request Endpoints

#### 1. Create Request
```
POST /api/rooms/{roomId}/requests
Body: {
  "userId": Long,
  "message": String (optional, max 500 chars)
}
Response: RoomJoinRequest
Errors:
  - 400: Room is "open" type (cannot create request)
  - 400: Room is closed
  - 400: User already a collaborator
  - 400: Pending request already exists
  - 400: Cooldown active (rejected < 5 min ago)
  - 404: Room not found
```

#### 2. Approve Request
```
POST /api/rooms/{roomId}/requests/{requestId}/approve
Body: {
  "adminId": Long
}
Response: 200 OK
Errors:
  - 403: Not admin or creator
  - 400: Request not pending
  - 400: User already collaborator
  - 400: Room at capacity
  - 404: Request not found
```

#### 3. Reject Request
```
POST /api/rooms/{roomId}/requests/{requestId}/reject
Body: {
  "adminId": Long,
  "reason": String (optional)
}
Response: 200 OK
Errors:
  - 403: Not admin or creator
  - 400: Request not pending
  - 404: Request not found
```

#### 4. Cancel Request
```
DELETE /api/rooms/{roomId}/requests/{requestId}
Query: userId=Long
Response: 200 OK
Errors:
  - 403: Not request owner
  - 400: Request not pending
  - 404: Request not found
```

#### 5. Get Pending Requests (Admin View)
```
GET /api/rooms/{roomId}/requests
Query: userId=Long
Response: List<RoomJoinRequest>
Errors:
  - 403: Not admin or creator
  - 404: Room not found
```

#### 6. Get My Pending Requests (Applicant View)
```
GET /api/rooms/my-applications
Query: userId=Long
Response: List<RoomJoinRequest>
```

#### 7. Close Room
```
POST /api/rooms/{roomId}/close
Body: {
  "userId": Long
}
Response: 200 OK
Errors:
  - 403: Not admin or creator
  - 404: Room not found
```

### Notification Endpoints

#### 1. Get Notifications
```
GET /api/notifications/{userId}
Response: List<Notification>
```

#### 2. Get Unread Notifications
```
GET /api/notifications/{userId}/unread
Response: List<Notification>
```

#### 3. Mark as Read
```
POST /api/notifications/{notificationId}/read
Query: userId=Long
Response: 200 OK
```

---

## Testing Strategy

### Unit Tests

1. **RoomJoinRequestService Tests**
   - Test request creation with all validation rules
   - Test approval with capacity checks
   - Test rejection with cooldown
   - Test cancellation
   - Test cooldown logic (5 minutes)

2. **NotificationService Tests**
   - Test notification creation with preferences
   - Test deduplication logic
   - Test recipient resolution
   - Test data schema generation

3. **RoomService Tests**
   - Test room closure logic
   - Test auto-close on event date
   - Test auto-reject on closure

### Integration Tests

1. **Request Lifecycle Tests**
   - Create → Approve → User becomes collaborator
   - Create → Reject → Cooldown → Reapply
   - Create → Cancel → Reapply immediately
   - Create → Room closes → Auto-reject

2. **Concurrency Tests**
   - Multiple admins approving simultaneously
   - Multiple users creating requests simultaneously
   - Capacity race condition handling

3. **Notification Tests**
   - Notification delivery with preferences
   - Deduplication effectiveness
   - Multiple recipients for new requests

### Performance Tests

1. **Load Tests**
   - 100 concurrent request creations
   - 50 concurrent approvals
   - Notification creation under load

2. **Database Tests**
   - Query performance with indexes
   - Cleanup job performance

---

## Success Criteria

### Functional Requirements
- ✅ All state transitions work correctly
- ✅ Cooldown mechanism works (5 minutes)
- ✅ Room closure auto-rejects requests
- ✅ Capacity reached auto-rejects requests
- ✅ Notifications sent to correct recipients
- ✅ Notification preferences respected
- ✅ Deduplication prevents spam

### Non-Functional Requirements
- ✅ API response time < 3 seconds (95th percentile)
- ✅ No race conditions in approval flow
- ✅ Database constraints prevent invalid states
- ✅ Test coverage > 80%
- ✅ All edge cases handled

---

## Risk Mitigation

### High Risk Items

1. **Race Conditions in Approval**
   - Mitigation: Database-level locking (SELECT FOR UPDATE)
   - Testing: Concurrent approval tests

2. **Notification Spam**
   - Mitigation: Deduplication hash + log
   - Testing: Rapid state change tests

3. **Data Migration Issues**
   - Mitigation: Test migration on staging first
   - Rollback plan: Keep backup before migration

4. **Performance Degradation**
   - Mitigation: Proper indexes, cleanup jobs
   - Monitoring: Query performance metrics

---

## Next Steps

1. **Review this plan** with the team
2. **Approve database migration** (`migration_v4.sql`)
3. **Start Phase 1** (Database Migration & Model Updates)
4. **Set up scheduled jobs** for cleanup tasks
5. **Create feature branch** for implementation

---

## Questions & Clarifications

If any questions arise during implementation, refer to the requirements clarification document or escalate to the product owner.

---

**Document Version:** 1.0  
**Last Updated:** [Current Date]  
**Author:** Senior Backend Architect  
**Status:** ✅ Ready for Implementation

