-- Creates zones and pricing rules for the SwiftDrop pricing engine.

CREATE TABLE zones (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    district VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT REFERENCES zones(id),
    size_factor_s DECIMAL(10,2) DEFAULT 1.00 NOT NULL,
    size_factor_m DECIMAL(10,2) DEFAULT 1.50 NOT NULL,
    size_factor_l DECIMAL(10,2) DEFAULT 2.00 NOT NULL,
    base_fee DECIMAL(10,2) NOT NULL,
    per_kg_fee DECIMAL(10,2) NOT NULL,
    inter_zone_surcharge DECIMAL(10,2) NOT NULL
);

-- Seed initial zones for the demo
INSERT INTO zones (name, district) VALUES 
('Colombo Zone', 'Colombo'),
('Gampaha Zone', 'Gampaha'),
('Kandy Zone', 'Kandy'),
('Galle Zone', 'Galle'),
('Jaffna Zone', 'Jaffna');

-- Default pricing rule (attached to Colombo as default)
INSERT INTO pricing_rules (zone_id, base_fee, per_kg_fee, inter_zone_surcharge) 
VALUES (1, 300.00, 100.00, 250.00);
