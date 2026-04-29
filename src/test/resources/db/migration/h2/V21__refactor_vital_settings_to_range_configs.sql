-- Rename table
ALTER TABLE vital_settings RENAME TO vital_range_configs;

-- Rename column
ALTER TABLE vital_range_configs RENAME COLUMN vital_name TO vital_type;

-- Update existing data to match Enum names
UPDATE vital_range_configs SET vital_type = 'BODY_TEMPERATURE' WHERE vital_type = 'TEMPERATURE';
UPDATE vital_range_configs SET vital_type = 'SYSTOLIC_BP' WHERE vital_type = 'SYSTOLIC';
UPDATE vital_range_configs SET vital_type = 'DIASTOLIC_BP' WHERE vital_type = 'DIASTOLIC';

-- Seed default values for all existing hospitals if not already present
-- H2 uses RANDOM_UUID() instead of gen_random_uuid()
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'BODY_TEMPERATURE', 95, 108, 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'BODY_TEMPERATURE');

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'SYSTOLIC_BP', 70, 220, 'mmHg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'SYSTOLIC_BP');

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'DIASTOLIC_BP', 40, 140, 'mmHg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'DIASTOLIC_BP');

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'OXYGEN_LEVEL', 50, 100, '%', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'OXYGEN_LEVEL');

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'WEIGHT', 1, 300, 'kg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'WEIGHT');

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'HEART_RATE', 30, 250, 'bpm', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hospital h
WHERE NOT EXISTS (SELECT 1 FROM vital_range_configs vrc WHERE vrc.hospital_id = h.id AND vrc.vital_type = 'HEART_RATE');
