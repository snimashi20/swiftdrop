-- A status change is no longer only the customer's business: the merchant, the assigned courier and
-- the dispatch/admin staff see it in the app too. One row per (event, recipient, channel), so the
-- table needs to know who the row is for; before this, "who" was implied by customer_id.
--
-- recipient_role is the role the recipient had when the row was written. It is denormalised on
-- purpose: it drives the wording of the feed item, and a later role change must not rewrite history.

ALTER TABLE notification_log ADD COLUMN recipient_user_id BIGINT;
ALTER TABLE notification_log ADD COLUMN recipient_role    VARCHAR(20);

-- Every existing row was written for the customer by the old consumer.
UPDATE notification_log
   SET recipient_user_id = customer_id,
       recipient_role    = 'CUSTOMER'
 WHERE recipient_user_id IS NULL;

ALTER TABLE notification_log ALTER COLUMN recipient_user_id SET NOT NULL;
ALTER TABLE notification_log ALTER COLUMN recipient_role    SET NOT NULL;

-- Uniqueness moves from (event, channel) to (event, recipient, channel): the same status change now
-- legitimately produces an IN_APP row for the customer, the merchant, the courier and each staff user.
ALTER TABLE notification_log DROP CONSTRAINT IF EXISTS uq_notification_log_event_type;
ALTER TABLE notification_log
    ADD CONSTRAINT uq_notification_log_event_recipient_type UNIQUE (event_id, recipient_user_id, type);

-- Serves the feed and the unread count, which are now keyed by recipient rather than by customer.
CREATE INDEX idx_notification_log_recipient ON notification_log (recipient_user_id);

DROP INDEX IF EXISTS idx_notification_log_customer_unread;
CREATE INDEX idx_notification_log_recipient_unread
    ON notification_log (recipient_user_id)
    WHERE read_at IS NULL;
