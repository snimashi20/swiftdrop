ALTER TABLE status_history
    ADD CONSTRAINT fk_status_history_parcel
    FOREIGN KEY (parcel_id) REFERENCES parcels (id) ON DELETE CASCADE;
