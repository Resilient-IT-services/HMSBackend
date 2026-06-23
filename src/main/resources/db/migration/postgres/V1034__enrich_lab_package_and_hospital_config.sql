-- Enrich lab_package with missing fields
ALTER TABLE lab.lab_package
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS package_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS version_no INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS is_billable BOOLEAN NOT NULL DEFAULT true;

-- Hospital-specific package configuration (mirrors lab_hospital_test_config for packages)
CREATE TABLE IF NOT EXISTS lab.lab_hospital_package_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    package_id UUID NOT NULL REFERENCES lab.lab_package(id) ON DELETE CASCADE,
    active BOOLEAN NOT NULL DEFAULT true,
    standard_price NUMERIC(10, 2),
    emergency_price NUMERIC(10, 2),
    UNIQUE (hospital_id, package_id)
);

-- Add findByHospitalIdAndPackageId support to lab_price (column already exists, just ensure index)
CREATE INDEX IF NOT EXISTS idx_lab_price_hospital_package ON lab.lab_price (hospital_id, package_id)
    WHERE package_id IS NOT NULL;
