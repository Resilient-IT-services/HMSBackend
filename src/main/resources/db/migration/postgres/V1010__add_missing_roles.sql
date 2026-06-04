-- V1010__add_missing_roles.sql
-- 1. Update the check constraint for RoleCode (Adding only LAB_TECHNICIAN)
ALTER TABLE iam.role DROP CONSTRAINT IF EXISTS chk_role_code;

ALTER TABLE iam.role ADD CONSTRAINT chk_role_code 
CHECK (code::text = ANY (ARRAY['ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'PHARMACIST', 'LAB_TECHNICIAN']::text[]));

-- 2. Insert missing roles
INSERT INTO iam.role (id, code, name, description, is_system, is_active, created_at, updated_at)
VALUES 
(gen_random_uuid(), 'LAB_TECHNICIAN', 'Lab Technician', 'Laboratory Technician', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
