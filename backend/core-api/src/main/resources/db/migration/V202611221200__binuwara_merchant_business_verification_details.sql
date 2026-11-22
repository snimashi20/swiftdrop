-- Optional at the schema level so existing merchant applications remain valid.
-- New registrations require these business verification details at the API boundary.
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS business_type VARCHAR(40);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS business_address VARCHAR(300);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS business_description VARCHAR(500);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS verification_declaration_accepted BOOLEAN;
