-- Migration v3: Add user ratings and fix enrollment type
-- Run this after migration_v2.sql

-- Create user_ratings table
CREATE TABLE IF NOT EXISTS user_ratings (
    id SERIAL PRIMARY KEY,
    rated_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rater_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score NUMERIC(3,1) NOT NULL CHECK (score >= 0 AND score <= 10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(rated_user_id, rater_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_ratings_rated_user_id ON user_ratings(rated_user_id);
CREATE INDEX IF NOT EXISTS idx_user_ratings_rater_user_id ON user_ratings(rater_user_id);
CREATE INDEX IF NOT EXISTS idx_user_ratings_created_at ON user_ratings(created_at);

-- Ensure rooms table has joinType column (should already exist from v2, but make sure)
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS "joinType" VARCHAR(50) DEFAULT 'open';
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS "updatedAt" TIMESTAMP;

-- Update joinType to use enum values: 'open' or 'by_application'
-- Ensure existing rooms have proper joinType
UPDATE rooms SET "joinType" = 'open' WHERE "joinType" IS NULL OR "joinType" = '';

-- Add about field to users table if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS about TEXT;

-- Ensure all users are members of default room (fix for 403 error)
INSERT INTO "roomsCollaborators" (roomId, colaboratorId)
SELECT r.id, u.id
FROM rooms r
CROSS JOIN users u
WHERE r.is_default = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM "roomsCollaborators" rc 
    WHERE rc.roomId = r.id AND rc.colaboratorId = u.id
  );

