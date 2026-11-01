-- V202610311500__binuwara_merchant_parcel_fk.sql
-- M9 (Binuwara): Foreign key from merchant_orders.parcel_id to parcels.id

ALTER TABLE merchant_orders
    ADD CONSTRAINT fk_merchant_orders_parcel
    FOREIGN KEY (parcel_id) REFERENCES parcels (id);
