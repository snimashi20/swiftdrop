-- V202611021200__binuwara_merchant_user_fks.sql
-- M9 (Binuwara): Foreign keys from merchants.user_id and merchants.reviewed_by to users.id

ALTER TABLE merchants
    ADD CONSTRAINT fk_merchants_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE merchants
    ADD CONSTRAINT fk_merchants_reviewed_by
    FOREIGN KEY (reviewed_by) REFERENCES users (id);
