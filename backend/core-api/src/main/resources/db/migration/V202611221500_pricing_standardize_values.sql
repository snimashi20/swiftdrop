-- Standardize pricing defaults for zones that still have the dirty seed values.
-- We explicitly do NOT update rows that look like deliberate admin edits (e.g., 250, 275).
UPDATE pricing_rules
SET base_fee = 200.00,
    per_kg_fee = 20.00,
    inter_zone_surcharge = 10.00
WHERE base_fee NOT IN (200.00, 250.00, 275.00)
   OR per_kg_fee <> 20.00
   OR inter_zone_surcharge <> 10.00;
