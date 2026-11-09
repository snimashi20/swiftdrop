-- Test-only copy of the tables core-api's Flyway creates (users from V202610261100, notification_log after V202611041000).
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_log (
    id              BIGSERIAL       PRIMARY KEY,
    event_id        UUID            NOT NULL,
    parcel_id       BIGINT          NOT NULL,
    tracking_number VARCHAR(12)     NOT NULL,
    old_status      VARCHAR(30),
    new_status      VARCHAR(30)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    recipient       VARCHAR(320)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    read_at         TIMESTAMPTZ,
    CONSTRAINT uq_notification_log_event_type UNIQUE (event_id, type)
);
