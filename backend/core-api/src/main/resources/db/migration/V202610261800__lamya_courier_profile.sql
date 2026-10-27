-- M6 (Lamya): dispatcher module.
-- 1) courier_profiles (new table)
-- 2) assignment columns added to M3's parcels table (additive only; M3's migration is untouched)

CREATE TABLE courier_profiles (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL UNIQUE,   -- the COURIER user (users.id)
    vehicle_type  VARCHAR(30)  NOT NULL,          -- BICYCLE, MOTORBIKE, THREE_WHEELER, CAR, VAN
    zone_id       BIGINT       NOT NULL,          -- zones.id (M4)
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_courier_profiles_zone_active ON courier_profiles (zone_id, active);

-- Assignment fields on parcels (parcels is created in V202610261500__isuri_parcels.sql)
ALTER TABLE parcels
    ADD COLUMN assigned_courier_id BIGINT,        -- courier_profiles.id, NULL = unassigned
    ADD COLUMN assigned_by_user_id BIGINT,        -- users.id of the dispatcher/admin
    ADD COLUMN assigned_at         TIMESTAMPTZ;

ALTER TABLE parcels
    ADD CONSTRAINT fk_parcels_assigned_courier
    FOREIGN KEY (assigned_courier_id) REFERENCES courier_profiles (id);

CREATE INDEX idx_parcels_assigned_courier ON parcels (assigned_courier_id, status);
CREATE INDEX idx_parcels_pickup_district  ON parcels (pickup_district, created_at);

-- ---- FKs to tables that do not exist in develop yet: uncomment ONLY after they are merged ----
-- ALTER TABLE courier_profiles ADD CONSTRAINT fk_courier_profiles_user FOREIGN KEY (user_id) REFERENCES users (id);
-- ALTER TABLE courier_profiles ADD CONSTRAINT fk_courier_profiles_zone FOREIGN KEY (zone_id) REFERENCES zones (id);
-- ALTER TABLE parcels          ADD CONSTRAINT fk_parcels_assigned_by    FOREIGN KEY (assigned_by_user_id) REFERENCES users (id);