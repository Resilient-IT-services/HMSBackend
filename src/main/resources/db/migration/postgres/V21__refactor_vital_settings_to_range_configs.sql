-- Rename table
ALTER TABLE vital_settings RENAME TO vital_range_configs;

-- Rename column
ALTER TABLE vital_range_configs RENAME COLUMN vital_name TO vital_type;

-- Update existing data to match Enum names
UPDATE vital_range_configs SET vital_type = 'BODY_TEMPERATURE' WHERE vital_type = 'TEMPERATURE';
UPDATE vital_range_configs SET vital_type = 'SYSTOLIC_BP' WHERE vital_type = 'SYSTOLIC';
UPDATE vital_range_configs SET vital_type = 'DIASTOLIC_BP' WHERE vital_type = 'DIASTOLIC';
-- HEART_RATE, WEIGHT, OXYGEN_LEVEL are already correct or similar enough if they existed.

-- Seed default values for all existing hospitals if not already present
-- BODY_TEMPERATURE: 95-108
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT gen_random_uuid(), h.id, 'BODY_TEMPERATURE', 95, 108, 'F', now(), now()
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'BODY_TEMPERATURE');

-- SYSTOLIC_BP: 70-220
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT gen_random_uuid(), h.id, 'SYSTOLIC_BP', 70, 220, 'mmHg', now(), now()
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'SYSTOLIC_BP');

-- DIASTOLIC_BP: 40-140
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT gen_random_uuid(), h.id, 'DIASTOLIC_BP', 40, 140, 'mmHg', now(), now()
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'DIASTOLIC_BP');

-- OXYGEN_LEVEL: 50-100
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT gen_random_uuid(), h.id, 'OXYGEN_LEVEL', 50, 100, '%', now(), now()
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'OXYGEN_LEVEL');

-- WEIGHT: 1-300
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT gen_random_uuid(), h.id, 'WEIGHT', 1, 300, 'kg', now(), now()
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'WEIGHT');

-- HEART_RATE: 30-250
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT gen_random_uuid(), h.id, 'HEART_RATE', 30, 250, 'bpm', now(), now()
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'HEART_RATE');
