-- The notification bell needs an unread count, so every delivery row records when the customer saw it.
-- NULL means unread. Rows of the same event are always marked together, so a status change is read as a whole.
ALTER TABLE notification_log ADD COLUMN read_at TIMESTAMPTZ;

-- Serves the unread count and the unread-only feed of one customer.
CREATE INDEX idx_notification_log_customer_unread
    ON notification_log (customer_id)
    WHERE read_at IS NULL;
