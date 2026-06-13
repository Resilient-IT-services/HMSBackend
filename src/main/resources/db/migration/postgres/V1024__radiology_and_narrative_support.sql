-- Phase A: Migration for Radiology and Narrative Support
ALTER TABLE lab.lab_test ADD COLUMN IF NOT EXISTS category VARCHAR(50);
ALTER TABLE lab.lab_test ADD COLUMN IF NOT EXISTS result_entry_type VARCHAR(50);

-- Phase D: Snapshot fields in lab_order_test
ALTER TABLE lab.lab_order_test ADD COLUMN IF NOT EXISTS test_name VARCHAR(255);
ALTER TABLE lab.lab_order_test ADD COLUMN IF NOT EXISTS category VARCHAR(50);
ALTER TABLE lab.lab_order_test ADD COLUMN IF NOT EXISTS result_entry_type VARCHAR(50);

-- Phase E: Narrative support in lab_result
ALTER TABLE lab.lab_result ADD COLUMN IF NOT EXISTS findings TEXT;
ALTER TABLE lab.lab_result ADD COLUMN IF NOT EXISTS order_test_id UUID REFERENCES lab.lab_order_test(id);
ALTER TABLE lab.lab_result ALTER COLUMN sample_id DROP NOT NULL;

-- Backfill order_test_id for existing results
UPDATE lab.lab_result r
SET order_test_id = s.order_test_id
FROM lab.lab_sample s
WHERE r.sample_id = s.id
AND r.order_test_id IS NULL;

-- Link PrescriptionLabItem to LabOrderTest
ALTER TABLE clinical.prescription_lab_item ADD COLUMN IF NOT EXISTS lab_order_test_id UUID;

-- Backfill data
UPDATE lab.lab_test 
SET category = 'LAB' 
WHERE category IS NULL;

UPDATE lab.lab_test 
SET result_entry_type = 'PARAMETER' 
WHERE result_entry_type IS NULL;

-- Backfill snapshots for existing orders
UPDATE lab.lab_order_test ot
SET 
    test_name = t.test_name,
    category = 'LAB',
    result_entry_type = 'PARAMETER'
FROM lab.lab_test t
WHERE ot.test_id = t.id 
AND ot.category IS NULL;
