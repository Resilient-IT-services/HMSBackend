-- V1026__extend_medicine_master_for_inventory.sql

-- 1. Add new columns to pharmacy.medicine (Global Catalog)
ALTER TABLE pharmacy.medicine ADD COLUMN IF NOT EXISTS item_type VARCHAR(20) DEFAULT 'MEDICINE';
ALTER TABLE pharmacy.medicine ADD COLUMN IF NOT EXISTS department VARCHAR(20);
ALTER TABLE pharmacy.medicine ADD COLUMN IF NOT EXISTS item_code VARCHAR(50);
ALTER TABLE pharmacy.medicine ADD COLUMN IF NOT EXISTS category VARCHAR(100);
ALTER TABLE pharmacy.medicine ADD COLUMN IF NOT EXISTS base_unit VARCHAR(50);
ALTER TABLE pharmacy.medicine ADD COLUMN IF NOT EXISTS is_batch_tracked BOOLEAN DEFAULT FALSE;
ALTER TABLE pharmacy.medicine ADD COLUMN IF NOT EXISTS is_expiry_tracked BOOLEAN DEFAULT FALSE;

-- 2. Relax existing constraints
ALTER TABLE pharmacy.medicine ALTER COLUMN form DROP NOT NULL;
ALTER TABLE pharmacy.medicine ALTER COLUMN composition DROP NOT NULL;

-- 3. Drop old unique constraint if exists
ALTER TABLE pharmacy.medicine DROP CONSTRAINT IF EXISTS uk_medicine_name_strength_form;

-- 4. Add new unique constraints (Global level)
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_item_code ON pharmacy.medicine(item_code) 
WHERE deleted = false AND item_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_dept_item_name ON pharmacy.medicine(department, name) 
WHERE deleted = false AND item_type = 'CONSUMABLE';

-- 5. Update existing records to have item_type 'MEDICINE'
UPDATE pharmacy.medicine SET item_type = 'MEDICINE' WHERE item_type IS NULL;
