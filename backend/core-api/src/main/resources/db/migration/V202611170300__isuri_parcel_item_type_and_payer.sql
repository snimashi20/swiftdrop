-- What is inside the parcel and who pays for the delivery. Both are optional so that
-- parcels created earlier (and merchant API calls that do not send them) keep working.
ALTER TABLE parcels ADD COLUMN IF NOT EXISTS item_type VARCHAR(30);
ALTER TABLE parcels ADD COLUMN IF NOT EXISTS delivery_paid_by VARCHAR(10);
