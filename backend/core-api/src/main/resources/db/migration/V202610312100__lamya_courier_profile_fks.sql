-- M6 (Lamya): foreign keys that had to wait until the other modules' tables existed.
-- users:  V202610261100__sadeepa_users_and_profiles.sql
-- zones:  V202610261700__naviru_pricing.sql
-- parcels: V202610261500__isuri_parcels.sql (assigned_by_user_id was added in V202610261800)

ALTER TABLE courier_profiles
    ADD CONSTRAINT fk_courier_profiles_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE courier_profiles
    ADD CONSTRAINT fk_courier_profiles_zone
    FOREIGN KEY (zone_id) REFERENCES zones (id);

ALTER TABLE parcels
    ADD CONSTRAINT fk_parcels_assigned_by
    FOREIGN KEY (assigned_by_user_id) REFERENCES users (id);