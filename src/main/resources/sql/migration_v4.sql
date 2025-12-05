-- Migration v4: Enhanced Room Join Request System & Notification System
-- Run this after migration_v3.sql
-- 
-- This migration adds:
-- 1. Enhanced room_join_requests with cooldown tracking and rejection reason
-- 2. Room closure status tracking
-- 3. Notification deduplication
-- 4. Notification retention policy support
-- 5. Standardized notification data schema
-- 6. User-level notification preferences for membership requests
-- 7. Room-level notification settings for membership requests

-- ============================================================================
-- 1. ENHANCE room_join_requests TABLE
-- ============================================================================

-- Add rejection reason field
ALTER TABLE room_join_requests ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Add last_rejected_at for cooldown tracking (5 minutes cooldown after rejection)
ALTER TABLE room_join_requests ADD COLUMN IF NOT EXISTS last_rejected_at TIMESTAMP;

-- Add index for cooldown queries (find recent rejections)
CREATE INDEX IF NOT EXISTS idx_room_join_requests_user_rejected 
    ON room_join_requests("userId", last_rejected_at) 
    WHERE status = 'rejected';

-- Add composite unique index to prevent duplicate pending requests
CREATE UNIQUE INDEX IF NOT EXISTS idx_room_join_requests_unique_pending 
    ON room_join_requests("roomId", "userId") 
    WHERE status = 'pending';

-- Add index for room status queries (find all pending requests for a room)
CREATE INDEX IF NOT EXISTS idx_room_join_requests_room_status 
    ON room_join_requests("roomId", status, "createdAt");

-- ============================================================================
-- 2. ENHANCE rooms TABLE
-- ============================================================================

-- Add room closure status (closed = true means room is closed, no new requests should be accepted)
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS is_closed BOOLEAN DEFAULT FALSE;

-- Add closed_at timestamp
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP;

-- Add closed_by user ID (who closed it, if manually closed)
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS closed_by INTEGER REFERENCES users(id);

-- Add index for closed rooms
CREATE INDEX IF NOT EXISTS idx_rooms_is_closed ON rooms(is_closed);

-- Add check constraint: joinType cannot be changed after creation (enforced at application level)
-- Note: This is a business rule, but we add a comment for documentation
COMMENT ON COLUMN rooms."joinType" IS 'Room type: "open" or "by_application". Cannot be changed after creation.';

-- ============================================================================
-- 3. ENHANCE notifications TABLE
-- ============================================================================

-- Add notification deduplication hash (MD5 hash of: userId + type + data)
-- This prevents duplicate notifications within a short time window
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deduplication_hash VARCHAR(64);

-- Add index for deduplication lookups
CREATE INDEX IF NOT EXISTS idx_notifications_deduplication 
    ON notifications(deduplication_hash, "createdAt");

-- Add index for retention policy cleanup (delete notifications older than 10 days)
CREATE INDEX IF NOT EXISTS idx_notifications_retention 
    ON notifications("createdAt") 
    WHERE "createdAt" < NOW() - INTERVAL '10 days';

-- Ensure type column has proper constraints
ALTER TABLE notifications ALTER COLUMN type SET NOT NULL;

-- Add check constraint for notification types
ALTER TABLE notifications ADD CONSTRAINT chk_notification_type 
    CHECK (type IN (
        'MEMBERSHIP_REQUEST',
        'MEMBERSHIP_APPROVED', 
        'MEMBERSHIP_REJECTED',
        'MEMBERSHIP_CANCELLED',
        'ROOM_CLOSED',
        'ROOM_CAPACITY_REACHED',
        'USER_BANNED',
        'POST_PINNED',
        'COMMENT',
        'MENTION',
        'EVENT_REMINDER_1H',
        'EVENT_REMINDER_24H'
    ));

-- ============================================================================
-- 4. ENHANCE user_settings TABLE
-- ============================================================================

-- Add membership request notification preferences
ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS membership_request_notifications BOOLEAN DEFAULT TRUE;
ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS membership_decision_notifications BOOLEAN DEFAULT TRUE;

-- Add index for notification preference queries
CREATE INDEX IF NOT EXISTS idx_user_settings_notifications 
    ON user_settings("userId", membership_request_notifications, membership_decision_notifications);

-- ============================================================================
-- 5. ENHANCE room_notification_settings TABLE
-- ============================================================================

-- Add membership request notification setting (if not exists)
ALTER TABLE room_notification_settings ADD COLUMN IF NOT EXISTS membership_request_notifications BOOLEAN DEFAULT TRUE;

-- Add index for room notification settings queries
CREATE INDEX IF NOT EXISTS idx_room_notification_settings_room 
    ON room_notification_settings("roomId");

-- ============================================================================
-- 6. CREATE notification_deduplication_log TABLE
-- ============================================================================
-- This table tracks recent notifications to prevent duplicates
-- Records older than 1 hour are automatically cleaned up

CREATE TABLE IF NOT EXISTS notification_deduplication_log (
    id SERIAL PRIMARY KEY,
    deduplication_hash VARCHAR(64) NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(deduplication_hash, user_id, notification_type)
);

CREATE INDEX IF NOT EXISTS idx_notification_dedup_hash 
    ON notification_deduplication_log(deduplication_hash, user_id, notification_type);

CREATE INDEX IF NOT EXISTS idx_notification_dedup_cleanup 
    ON notification_deduplication_log(created_at) 
    WHERE created_at < NOW() - INTERVAL '1 hour';

-- ============================================================================
-- 7. CREATE room_join_request_history TABLE (Optional - for audit trail)
-- ============================================================================
-- This table tracks all state changes to requests for audit purposes
-- Useful for debugging and understanding request lifecycle

CREATE TABLE IF NOT EXISTS room_join_request_history (
    id SERIAL PRIMARY KEY,
    request_id INTEGER NOT NULL REFERENCES room_join_requests(id) ON DELETE CASCADE,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by INTEGER REFERENCES users(id),
    change_reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_request_history_request_id 
    ON room_join_request_history(request_id, changed_at);

CREATE INDEX IF NOT EXISTS idx_request_history_changed_by 
    ON room_join_request_history(changed_by);

-- ============================================================================
-- 8. DATA MIGRATION: Set default values for existing records
-- ============================================================================

-- Set default notification preferences for existing users
UPDATE user_settings 
SET membership_request_notifications = TRUE,
    membership_decision_notifications = TRUE
WHERE membership_request_notifications IS NULL 
   OR membership_decision_notifications IS NULL;

-- Set default room notification settings for existing rooms
UPDATE room_notification_settings
SET membership_request_notifications = TRUE
WHERE membership_request_notifications IS NULL;

-- Mark all existing rooms as not closed (unless they have passed event date)
-- Note: This assumes meetingTime exists and is in the past
UPDATE rooms 
SET is_closed = FALSE
WHERE is_closed IS NULL;

-- Set closed_at for rooms where meetingTime has passed
UPDATE rooms 
SET is_closed = TRUE,
    closed_at = meetingTime
WHERE meetingTime IS NOT NULL 
  AND meetingTime < NOW() 
  AND is_closed = FALSE;

-- ============================================================================
-- 9. CREATE FUNCTIONS FOR AUTOMATED CLEANUP
-- ============================================================================

-- Function to clean up old notifications (older than 10 days)
CREATE OR REPLACE FUNCTION cleanup_old_notifications()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM notifications 
    WHERE "createdAt" < NOW() - INTERVAL '10 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up old deduplication log entries (older than 1 hour)
CREATE OR REPLACE FUNCTION cleanup_deduplication_log()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM notification_deduplication_log 
    WHERE created_at < NOW() - INTERVAL '1 hour';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 10. CREATE TRIGGERS FOR AUTOMATED ACTIONS
-- ============================================================================

-- Trigger to auto-close rooms when meetingTime passes
CREATE OR REPLACE FUNCTION auto_close_room_on_event_date()
RETURNS TRIGGER AS $$
BEGIN
    -- If meetingTime is in the past and room is not already closed
    IF NEW.meetingTime IS NOT NULL 
       AND NEW.meetingTime < NOW() 
       AND (OLD.meetingTime IS NULL OR OLD.meetingTime >= NOW())
       AND NEW.is_closed = FALSE THEN
        NEW.is_closed := TRUE;
        NEW.closed_at := NEW.meetingTime;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_auto_close_room
    BEFORE INSERT OR UPDATE ON rooms
    FOR EACH ROW
    EXECUTE FUNCTION auto_close_room_on_event_date();

-- ============================================================================
-- 11. COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE room_join_requests IS 'Tracks user requests to join rooms with "by_application" type. States: pending, approved, rejected, cancelled.';
COMMENT ON COLUMN room_join_requests.last_rejected_at IS 'Timestamp of last rejection. Used to enforce 5-minute cooldown before user can reapply.';
COMMENT ON COLUMN room_join_requests.rejection_reason IS 'Optional reason for rejection, provided by admin.';
COMMENT ON COLUMN rooms.is_closed IS 'If true, room is closed and no new requests should be accepted. Auto-closed when event date passes.';
COMMENT ON COLUMN rooms.closed_at IS 'Timestamp when room was closed (either manually or automatically).';
COMMENT ON COLUMN notifications.deduplication_hash IS 'MD5 hash of (userId + type + data) to prevent duplicate notifications.';
COMMENT ON TABLE notification_deduplication_log IS 'Temporary log to prevent duplicate notifications within 1 hour window. Auto-cleaned.';
COMMENT ON TABLE room_join_request_history IS 'Audit trail of all state changes to room join requests.';

