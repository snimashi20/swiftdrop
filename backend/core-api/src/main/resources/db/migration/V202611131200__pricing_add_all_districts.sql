-- Add all 25 districts to the zones table
INSERT INTO zones (name, district) VALUES 
('Ampara Zone', 'Ampara'),
('Anuradhapura Zone', 'Anuradhapura'),
('Badulla Zone', 'Badulla'),
('Batticaloa Zone', 'Batticaloa'),
('Colombo Zone', 'Colombo'),
('Galle Zone', 'Galle'),
('Gampaha Zone', 'Gampaha'),
('Hambantota Zone', 'Hambantota'),
('Jaffna Zone', 'Jaffna'),
('Kalutara Zone', 'Kalutara'),
('Kandy Zone', 'Kandy'),
('Kegalle Zone', 'Kegalle'),
('Kilinochchi Zone', 'Kilinochchi'),
('Kurunegala Zone', 'Kurunegala'),
('Mannar Zone', 'Mannar'),
('Matale Zone', 'Matale'),
('Matara Zone', 'Matara'),
('Monaragala Zone', 'Monaragala'),
('Mullaitivu Zone', 'Mullaitivu'),
('Nuwara Eliya Zone', 'Nuwara Eliya'),
('Polonnaruwa Zone', 'Polonnaruwa'),
('Puttalam Zone', 'Puttalam'),
('Ratnapura Zone', 'Ratnapura'),
('Trincomalee Zone', 'Trincomalee'),
('Vavuniya Zone', 'Vavuniya')
ON CONFLICT (district) DO NOTHING;

-- Seed a default pricing rule for any zone that doesn't have one
INSERT INTO pricing_rules (zone_id, base_fee, per_kg_fee, inter_zone_surcharge, size_factor_s, size_factor_m, size_factor_l)
SELECT id, 300.00, 100.00, 250.00, 1.00, 1.50, 2.00 FROM zones 
WHERE id NOT IN (SELECT zone_id FROM pricing_rules);
