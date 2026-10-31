-- Adds the pickup zone to parcels (used by dispatcher zone filter and courier suggestion).
-- Nullable: existing rows stay NULL. Set at booking from pickup_district via zones.district.

ALTER TABLE parcels
    ADD COLUMN pickup_zone_id BIGINT;

ALTER TABLE parcels
    ADD CONSTRAINT fk_parcels_pickup_zone
    FOREIGN KEY (pickup_zone_id) REFERENCES zones (id);

CREATE INDEX idx_parcels_pickup_zone ON parcels (pickup_zone_id);