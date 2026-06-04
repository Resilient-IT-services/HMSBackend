-- V1011__drop_hsn_master_and_cleanup_medicine_tax.sql
-- Drop redundant HSN master table and tax fields from pharmacy module
-- These are now handled by the Tax Engine (V1003) and Medicine table directly

-- 1. Drop HSN Master table
DROP TABLE IF EXISTS pharmacy.hsn_master;

-- 2. Remove redundant tax_percentage from medicine
ALTER TABLE pharmacy.medicine DROP COLUMN IF EXISTS tax_percentage;

-- 3. Remove redundant tax_percentage from hospital_medicine_catalog
-- Tax calculation is now performed by TaxEngineService using HSN codes
ALTER TABLE pharmacy.hospital_medicine_catalog DROP COLUMN IF EXISTS tax_percentage;
