CREATE TABLE status_history (
    id                 BIGSERIAL PRIMARY KEY,
    parcel_id          BIGINT       NOT NULL,
    old_status         VARCHAR(20),
    new_status         VARCHAR(20)  NOT NULL,
    changed_by_user_id BIGINT,
    changed_by_role    VARCHAR(20)  NOT NULL,
    note               VARCHAR(500),
    changed_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_status_history_parcel
    ON status_history (parcel_id, changed_at);