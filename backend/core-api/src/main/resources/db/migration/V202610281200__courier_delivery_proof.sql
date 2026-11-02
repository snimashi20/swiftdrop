-- 1. Additional courier delivery & failure columns on parcels
ALTER TABLE parcels ADD COLUMN IF NOT EXISTS delivery_instructions VARCHAR(500);
ALTER TABLE parcels ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(50);
ALTER TABLE parcels ADD COLUMN IF NOT EXISTS failure_note VARCHAR(500);

-- 2. Delivery Proof table for photo proof and recipient signature/name
CREATE TABLE IF NOT EXISTS delivery_proofs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parcel_id       BIGINT NOT NULL UNIQUE,
    recipient_name  VARCHAR(100) NOT NULL,
    photo_path      VARCHAR(255),
    note            VARCHAR(500),
    delivered_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_delivery_proof_parcel FOREIGN KEY (parcel_id) REFERENCES parcels(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_delivery_proofs_parcel_id ON delivery_proofs(parcel_id);