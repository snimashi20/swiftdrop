-- Standardize pricing defaults for zones that still have the dirty seed values.
-- We ONLY update if ALL THREE fields match the dirty seed patterns (e.g., 999 / 20 / 10)
-- This ensures deliberate admin edits (e.g., base fee 250 with a custom per_kg of 25) are NEVER overwritten.
UPDATE pricing_rules
SET base_fee = 200.00,
    per_kg_fee = 20.00,
    inter_zone_surcharge = 10.00
WHERE (base_fee = 999.00 OR base_fee = 100.00 OR base_fee = 20.00)
  AND per_kg_fee = 20.00
  AND inter_zone_surcharge = 10.00;
