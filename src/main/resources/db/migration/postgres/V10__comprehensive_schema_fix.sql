-- V10__comprehensive_schema_fix.sql
-- This migration consolidates all missing columns identified during startup validation errors.

-- 1. Appointment denormalized names
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS patient_name varchar(255);
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS doctor_name varchar(255);

-- 2. Billing doctor info
ALTER TABLE billing ADD COLUMN IF NOT EXISTS doctor_id uuid;
ALTER TABLE billing ADD COLUMN IF NOT EXISTS doctor_name varchar(255);

-- 3. Prescription Item medicine name
ALTER TABLE prescription_item ADD COLUMN IF NOT EXISTS medicine_name varchar(255);

-- 4. Prescription audit columns
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS created_at timestamp;
ALTER TABLE prescription ADD COLUMN IF NOT EXISTS updated_at timestamp;

-- 5. Vitals audit columns
ALTER TABLE vitals ADD COLUMN IF NOT EXISTS created_at timestamp;
ALTER TABLE vitals ADD COLUMN IF NOT EXISTS updated_at timestamp;

-- 6. Doctor Schedules missing columns
ALTER TABLE doctor_schedules ADD COLUMN IF NOT EXISTS slot_duration_minutes integer DEFAULT 15;
ALTER TABLE doctor_schedules ADD COLUMN IF NOT EXISTS max_patients integer;

-- 7. Inventory Stock missing columns
ALTER TABLE inventory_stock ADD COLUMN IF NOT EXISTS last_updated timestamp;

-- Backfill data for audit columns where possible
UPDATE prescription p SET created_at = a.created_at, updated_at = a.updated_at 
FROM appointment a WHERE p.id = a.id AND p.created_at IS NULL;

UPDATE vitals v SET created_at = a.created_at, updated_at = a.updated_at 
FROM appointment a WHERE v.id = a.id AND v.created_at IS NULL;

UPDATE inventory_stock SET last_updated = updated_at WHERE last_updated IS NULL;

-- Set NOT NULL where required by entities
ALTER TABLE prescription ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE prescription ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE vitals ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE vitals ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE inventory_stock ALTER COLUMN last_updated SET NOT NULL;
