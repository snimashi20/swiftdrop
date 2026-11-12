-- V202610311300__sandavi_align_webhook_schema.sql
-- M10 (Sandavi): Align tracking_number length to VARCHAR(12) and add UNIQUE constraint on merchant_id

-- 1. Enforce 1-to-1 relationship between merchant and webhook_endpoint
ALTER TABLE webhook_endpoints
    ADD CONSTRAINT uq_webhook_endpoints_merchant_id UNIQUE (merchant_id);

-- 2. Align tracking_number length with parcels table (VARCHAR(12))
ALTER TABLE webhook_deliveries
    ALTER COLUMN tracking_number TYPE VARCHAR(12);
