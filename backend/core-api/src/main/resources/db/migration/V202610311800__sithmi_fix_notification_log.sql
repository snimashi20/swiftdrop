-- First status event of a parcel has oldStatus = null.
ALTER TABLE notification_log ALTER COLUMN old_status DROP NOT NULL;

-- One event produces one row per channel (EMAIL, SMS), so uniqueness is per (event_id, type).
ALTER TABLE notification_log DROP CONSTRAINT IF EXISTS notification_log_event_id_key;
ALTER TABLE notification_log ADD CONSTRAINT uq_notification_log_event_type UNIQUE (event_id, type);
