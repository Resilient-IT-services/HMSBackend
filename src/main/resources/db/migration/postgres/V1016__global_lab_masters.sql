-- Make hospital_id nullable for Global Masters in Lab module
ALTER TABLE lab.lab_department ALTER COLUMN hospital_id DROP NOT NULL;
ALTER TABLE lab.lab_test ALTER COLUMN hospital_id DROP NOT NULL;
ALTER TABLE lab.lab_package ALTER COLUMN hospital_id DROP NOT NULL;

-- Ensure constraints are global-friendly
-- lab_department.code and lab_test.test_code should be unique globally if hospital_id is null, 
-- or unique per hospital if hospital_id is present.
-- For simplicity in MVP, we keep the existing UNIQUE constraints if they were global, 
-- or modify them if they were per-hospital.

-- V1006 defined them as:
-- code VARCHAR(50) UNIQUE (for lab_department)
-- test_code VARCHAR(50) UNIQUE NOT NULL (for lab_test)
-- These are already global uniques, which is good for Global Masters.
