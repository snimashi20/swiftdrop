-- Migration for user account status and approval workflow
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

-- Ensure all existing users are set to ACTIVE if null
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;

