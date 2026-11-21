-- A status change is no longer only the customer's business: the merchant, the assigned courier and
-- the dispatch/admin staff see it in the app too. One row per (event, recipient, channel), so the
-- table needs to know who the row is for; before this, "who" was implied by customer_id.
--
-- recipient_role is the role the recipient had when the row was written. It is denormalised on
-- purpose: it drives the wording of the feed item, and a later role change must not rewrite history.
--
-- DELIBERATELY NULLABLE. We all share one Flyway history on the team database, so this migration
-- reaches everyone the moment it merges - including teammates still running the previous consumer,
-- whose entity does not set these columns. A NOT NULL without a default would make every one of
-- their notification inserts fail. The NOT NULL tightening is a follow-up migration, to be merged
-- once the whole team is on this code. Until then nullable is the compatible state, and a NULL
-- recipient_user_id simply means a row written by the old consumer.

ALTER TABLE notification_log ADD COLUMN IF NOT EXISTS recipient_user_id BIGINT;
ALTER TABLE notification_log ADD COLUMN IF NOT EXISTS recipient_role    VARCHAR(20);

-- Every existing row was written for the customer by the old consumer.
UPDATE notification_log
   SET recipient_user_id = customer_id,
       recipient_role    = 'CUSTOMER'
 WHERE recipient_user_id IS NULL;

-- Uniqueness moves from (event, channel) to (event, recipient, channel): the same status change now
-- legitimately produces an IN_APP row for the customer, the merchant, the courier and each staff user.
-- Postgres does not treat two NULLs as equal, so this never blocks a row from the old consumer.
ALTER TABLE notification_log DROP CONSTRAINT IF EXISTS uq_notification_log_event_type;
ALTER TABLE notification_log DROP CONSTRAINT IF EXISTS uq_notification_log_event_recipient_type;
ALTER TABLE notification_log
    ADD CONSTRAINT uq_notification_log_event_recipient_type UNIQUE (event_id, recipient_user_id, type);

-- Serves the feed and the unread count, which are now keyed by recipient rather than by customer.
CREATE INDEX IF NOT EXISTS idx_notification_log_recipient ON notification_log (recipient_user_id);

DROP INDEX IF EXISTS idx_notification_log_customer_unread;
CREATE INDEX IF NOT EXISTS idx_notification_log_recipient_unread
    ON notification_log (recipient_user_id)
    WHERE read_at IS NULL;
