CREATE TABLE notification_log (
    id              BIGSERIAL       PRIMARY KEY,
    event_id        UUID            NOT NULL UNIQUE,
    parcel_id       BIGINT          NOT NULL,
    tracking_number VARCHAR(12)     NOT NULL,
    old_status      VARCHAR(30)     NOT NULL,
    new_status      VARCHAR(30)     NOT NULL,
    customer_id     BIGINT          NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    recipient       VARCHAR(320)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ
);

CREATE INDEX idx_notification_log_customer   ON notification_log (customer_id);
CREATE INDEX idx_notification_log_parcel     ON notification_log (parcel_id);
CREATE INDEX idx_notification_log_event_id   ON notification_log (event_id);
