-- V202611141000__courier_applications.sql
-- Courier approval workflow: admins review rider applications before a COURIER
-- account is activated. Mirrors the merchants approval schema.

CREATE TABLE IF NOT EXISTS courier_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(30),
    license_number VARCHAR(100),
    vehicle_type VARCHAR(30),
    preferred_zone VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    reviewed_by BIGINT,
    CONSTRAINT fk_courier_applications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_courier_applications_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_courier_applications_status ON courier_applications(status);

-- Backfill applications for courier accounts that registered before this table existed,
-- so the admin review queue is not empty for users already waiting.
INSERT INTO courier_applications (user_id, full_name, contact_email, contact_phone, status, created_at)
SELECT u.id,
       COALESCE(NULLIF(TRIM(u.full_name), ''), u.email),
       u.email,
       u.phone_number,
       CASE
           WHEN UPPER(COALESCE(u.status, 'ACTIVE')) = 'PENDING_APPROVAL' THEN 'PENDING'
           WHEN UPPER(COALESCE(u.status, 'ACTIVE')) = 'REJECTED' THEN 'REJECTED'
           ELSE 'APPROVED'
       END,
       COALESCE(u.created_at, CURRENT_TIMESTAMP)
FROM users u
WHERE REPLACE(UPPER(u.role), 'ROLE_', '') = 'COURIER'
ON CONFLICT (user_id) DO NOTHING;
