-- M7: Add courier onboarding verification & NIC document columns (nullable so existing demo/dispatcher rows stay valid)
ALTER TABLE courier_profiles
    ADD COLUMN IF NOT EXISTS nic_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS residential_address VARCHAR(300),
    ADD COLUMN IF NOT EXISTS driving_license_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS driving_license_expiry DATE,
    ADD COLUMN IF NOT EXISTS vehicle_plate_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS nic_front_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS nic_back_photo_url VARCHAR(500);