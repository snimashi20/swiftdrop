-- V202610261600__binuwara_merchants.sql
-- M9 (Binuwara): Merchant, ApiKey, and MerchantOrder schema

-- 1. merchants table
CREATE TABLE IF NOT EXISTS merchants (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    business_name VARCHAR(150) NOT NULL,
    registration_number VARCHAR(100) NOT NULL UNIQUE,
    contact_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    reviewed_by BIGINT
    -- FKs for user_id and reviewed_by will be added in a later migration once the users table exists.
);

CREATE INDEX IF NOT EXISTS idx_merchants_status ON merchants(status);

-- 2. api_keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    label VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_api_keys_merchant_id ON api_keys(merchant_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_key_hash ON api_keys(key_hash);

-- 3. merchant_orders table (idempotency + link to parcels)
CREATE TABLE IF NOT EXISTS merchant_orders (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    parcel_id BIGINT,
    idempotency_key VARCHAR(255) NOT NULL,
    external_order_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_merchant_orders_merchant_idempotency UNIQUE (merchant_id, idempotency_key)
    -- FK for parcel_id will be added in a later migration once the parcels table exists.
);

CREATE INDEX IF NOT EXISTS idx_merchant_orders_merchant_id ON merchant_orders(merchant_id);
CREATE INDEX IF NOT EXISTS idx_merchant_orders_parcel_id ON merchant_orders(parcel_id);
