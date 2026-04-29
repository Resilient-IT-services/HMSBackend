
-- Remove NOT NULL constraint (PostgreSQL syntax)
ALTER TABLE doctor_profile
    ALTER COLUMN license_number DROP NOT NULL;

-- Convert empty strings to NULL
UPDATE doctor_profile
SET license_number = NULL
WHERE license_number IS NOT NULL
  AND TRIM(license_number) = '';
