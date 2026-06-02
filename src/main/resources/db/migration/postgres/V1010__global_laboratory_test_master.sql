-- V1010__global_laboratory_test_master.sql
-- Create Laboratory Schema and Global Test Master Table

CREATE SCHEMA IF NOT EXISTS laboratory;

-- 1. Laboratory Test Master Table (Global)
CREATE TABLE IF NOT EXISTS laboratory.laboratory_test (
    id UUID PRIMARY KEY,
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    short_name VARCHAR(100),
    alias_name VARCHAR(255),
    active_status BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Cleanup if hospital_id was added in a previous failed attempt
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'laboratory' 
               AND table_name = 'laboratory_test' 
               AND column_name = 'hospital_id') THEN
        ALTER TABLE laboratory.laboratory_test DROP COLUMN hospital_id;
    END IF;
END $$;

-- 3. Constraints and Indexes
-- Unique Test Code (Case-insensitive)
DROP INDEX IF EXISTS laboratory.idx_lab_test_code_unique;
CREATE UNIQUE INDEX idx_lab_test_code_unique ON laboratory.laboratory_test (LOWER(test_code));

-- Unique Test Name (Case-insensitive)
DROP INDEX IF EXISTS laboratory.idx_lab_test_name_unique;
CREATE UNIQUE INDEX idx_lab_test_name_unique ON laboratory.laboratory_test (LOWER(test_name));
