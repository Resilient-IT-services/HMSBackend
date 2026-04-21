-- Migration to remove hospital_id from doctor and nurse tables and enforce one profile per user

-- 1. Clean up duplicate doctor records (keep most recent)
DELETE FROM doctor WHERE id NOT IN (
    SELECT MAX(id) FROM doctor GROUP BY user_id
);

-- 2. Clean up duplicate nurse records (keep most recent)
DELETE FROM nurse WHERE id NOT IN (
    SELECT MAX(id) FROM nurse GROUP BY user_id
);

-- 3. Remove hospital_id from doctor and nurse
ALTER TABLE doctor DROP COLUMN hospital_id;
ALTER TABLE nurse DROP COLUMN hospital_id;

-- 4. Ensure user_id is NOT NULL and UNIQUE for both tables
-- First, make sure they are not null (they should have been linked in V3)
ALTER TABLE doctor ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE nurse ALTER COLUMN user_id SET NOT NULL;

-- Add unique constraint
ALTER TABLE doctor ADD CONSTRAINT uc_doctor_user UNIQUE (user_id);
ALTER TABLE nurse ADD CONSTRAINT uc_nurse_user UNIQUE (user_id);
