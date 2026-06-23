-- Support packages in prescription_lab_item
ALTER TABLE clinical.prescription_lab_item ADD COLUMN IF NOT EXISTS package_id UUID;
ALTER TABLE clinical.prescription_lab_item ADD COLUMN IF NOT EXISTS package_name VARCHAR(255);

ALTER TABLE clinical.prescription_lab_item ALTER COLUMN test_id DROP NOT NULL;
ALTER TABLE clinical.prescription_lab_item ALTER COLUMN test_name DROP NOT NULL;
