-- V1004__laboratory_test_master.sql
-- Create Laboratory Schema and Global Test Master Table

CREATE SCHEMA IF NOT EXISTS laboratory;

-- 1. Laboratory Test Master Table (Global)
CREATE TABLE laboratory.laboratory_test (
    id UUID PRIMARY KEY,
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    short_name VARCHAR(100),
    alias_name VARCHAR(255),
    active_status BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Constraints and Indexes
-- Unique Test Code (Case-insensitive)
CREATE UNIQUE INDEX idx_lab_test_code_unique ON laboratory.laboratory_test (LOWER(test_code));

-- Unique Test Name (Case-insensitive)
CREATE UNIQUE INDEX idx_lab_test_name_unique ON laboratory.laboratory_test (LOWER(test_name));
