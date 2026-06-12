-- V1021__rename_lab_technician_to_lab_admin.sql

-- 1. Drop the check constraint first
ALTER TABLE iam.role DROP CONSTRAINT IF EXISTS chk_role_code;

-- 2. Update existing role record
UPDATE iam.role 
SET code = 'LAB_ADMIN', 
    name = 'Lab Admin', 
    description = 'Laboratory Administrator'
WHERE code = 'LAB_TECHNICIAN';

-- 3. Add the updated check constraint
ALTER TABLE iam.role ADD CONSTRAINT chk_role_code 
CHECK (code::text = ANY (ARRAY['ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'PHARMACIST', 'LAB_ADMIN', 'SUPER_ADMIN', 'PATHOLOGIST']::text[]));

-- 4. Rename lab_tech_profile table to lab_admin_profile
ALTER TABLE staff.lab_tech_profile RENAME TO lab_admin_profile;
