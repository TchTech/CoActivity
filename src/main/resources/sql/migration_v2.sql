-- Migration v2: Add new features and tables
-- Run this after init_tables.sql

-- Add is_default flag to rooms table
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_rooms_is_default ON rooms(is_default);

-- Update notifications table to add type and data fields
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type VARCHAR(50);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS data TEXT; -- JSON data
ALTER TABLE notifications ALTER COLUMN isRead TYPE BOOLEAN USING CASE WHEN isRead = 1 THEN TRUE ELSE FALSE END;
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(userId);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(createdAt);

-- Update room_join_requests table to add message, responded_at, responder_id
ALTER TABLE room_join_requests ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE room_join_requests ADD COLUMN IF NOT EXISTS responded_at TIMESTAMP;
ALTER TABLE room_join_requests ADD COLUMN IF NOT EXISTS responder_id INTEGER REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_room_join_requests_room_id ON room_join_requests(roomId);
CREATE INDEX IF NOT EXISTS idx_room_join_requests_user_id ON room_join_requests(userId);
CREATE INDEX IF NOT EXISTS idx_room_join_requests_status ON room_join_requests(status);

-- Update external_links table to add label, created_at, updated_at
ALTER TABLE external_links ADD COLUMN IF NOT EXISTS label VARCHAR(255);
ALTER TABLE external_links ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE external_links ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_external_links_user_id ON external_links(userId);

-- Create room_post_pin table
CREATE TABLE IF NOT EXISTS room_post_pin (
    id SERIAL PRIMARY KEY,
    room_id INTEGER NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    pinned_by INTEGER NOT NULL REFERENCES users(id),
    pinned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(room_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_room_post_pin_room_id ON room_post_pin(room_id);
CREATE INDEX IF NOT EXISTS idx_room_post_pin_post_id ON room_post_pin(post_id);
CREATE INDEX IF NOT EXISTS idx_room_post_pin_pinned_at ON room_post_pin(pinned_at);

-- Create default room and add all existing users
INSERT INTO rooms (name, description, category, "createdById", "createdAt", is_default, "joinType")
SELECT 'General', 'Default room for all users', 'General', 
       (SELECT id FROM users ORDER BY id LIMIT 1),
       CURRENT_TIMESTAMP, TRUE, 'open'
WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE is_default = TRUE);

-- Add all existing users to default room
INSERT INTO "roomsCollaborators" (roomId, colaboratorId)
SELECT r.id, u.id
FROM rooms r
CROSS JOIN users u
WHERE r.is_default = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM "roomsCollaborators" rc 
    WHERE rc.roomId = r.id AND rc.colaboratorId = u.id
  );

-- Add room creator as admin of default room
INSERT INTO "room_admins" (roomId, userId)
SELECT r.id, r."createdById"
FROM rooms r
WHERE r.is_default = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM "room_admins" ra 
    WHERE ra.roomId = r.id AND ra.userId = r."createdById"
  );

