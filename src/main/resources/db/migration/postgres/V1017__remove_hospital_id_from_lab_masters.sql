-- Completely remove hospital_id from Lab Masters to enforce 100% Global catalog
ALTER TABLE lab.lab_department DROP COLUMN IF EXISTS hospital_id;
ALTER TABLE lab.lab_test DROP COLUMN IF EXISTS hospital_id;
ALTER TABLE lab.lab_package DROP COLUMN IF EXISTS hospital_id;
