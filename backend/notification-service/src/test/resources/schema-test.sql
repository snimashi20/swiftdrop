-- Test-only copy of the tables core-api's Flyway creates. Keep in step with:
--   users           V202610261100 (+ status from V202611111200)
--   notification_log V202610262300 .. V202611141100
--   merchants       V202610261600 (only the columns the recipient lookup reads)
--   parcels         V202610261500 (only the columns the recipient lookup reads)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_log (
    id                BIGSERIAL       PRIMARY KEY,
    event_id          UUID            NOT NULL,
    parcel_id         BIGINT          NOT NULL,
    tracking_number   VARCHAR(12)     NOT NULL,
    old_status        VARCHAR(30),
    new_status        VARCHAR(30)     NOT NULL,
    customer_id       BIGINT          NOT NULL,
    recipient_user_id BIGINT          NOT NULL,
    recipient_role    VARCHAR(20)     NOT NULL,
    type              VARCHAR(20)     NOT NULL,
    recipient         VARCHAR(320)    NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    failure_reason        TEXT,
    parcel_failure_reason VARCHAR(50),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    sent_at           TIMESTAMPTZ,
    read_at           TIMESTAMPTZ,
    CONSTRAINT uq_notification_log_event_recipient_type UNIQUE (event_id, recipient_user_id, type)
);

-- The recipient lookup joins these two to find the merchant owner and the assigned courier.
CREATE TABLE IF NOT EXISTS merchants (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE
);

CREATE TABLE IF NOT EXISTS parcels (
    id                  BIGSERIAL PRIMARY KEY,
    assigned_courier_id BIGINT
);
