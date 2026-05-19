-- V26__membership_and_catalog_refactor.sql

-- 1. Reference Data Tables
CREATE TABLE IF NOT EXISTS department (
    id uuid PRIMARY KEY,
    name varchar(255) UNIQUE NOT NULL,
    description text,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS specialization (
    id uuid PRIMARY KEY,
    name varchar(255) UNIQUE NOT NULL,
    description text,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Insert Default Departments and Specializations
-- General Department
INSERT INTO department (id, name, description, is_active, created_at, updated_at)
VALUES (RANDOM_UUID(), 'General', 'General Department', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Cardiology Department
INSERT INTO department (id, name, description, is_active, created_at, updated_at)
VALUES (RANDOM_UUID(), 'Cardiology', 'Cardiology Department', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- General Medicine Specialization
INSERT INTO specialization (id, name, description, is_active, created_at, updated_at)
VALUES (RANDOM_UUID(), 'General Medicine', 'General Medicine', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Cardiology Specialization
INSERT INTO specialization (id, name, description, is_active, created_at, updated_at)
VALUES (RANDOM_UUID(), 'Cardiology', 'Cardiology', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Rename user_role_assignment to user_contract
ALTER TABLE user_role_assignment RENAME TO user_contract;

-- 4. Create Join Tables
CREATE TABLE IF NOT EXISTS user_contract_department (
    user_contract_id uuid NOT NULL,
    department_id uuid NOT NULL,
    CONSTRAINT pk_ucd PRIMARY KEY (user_contract_id, department_id),
    CONSTRAINT fk_ucd_contract FOREIGN KEY (user_contract_id) REFERENCES user_contract(id) ON DELETE CASCADE,
    CONSTRAINT fk_ucd_dept FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS hospital_departments (
    hospital_id uuid NOT NULL,
    department_id uuid NOT NULL,
    CONSTRAINT pk_hd PRIMARY KEY (hospital_id, department_id),
    CONSTRAINT fk_hd_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id) ON DELETE CASCADE,
    CONSTRAINT fk_hd_dept FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS doctor_specialization (
    doctor_profile_id uuid NOT NULL,
    specialization_id uuid NOT NULL,
    CONSTRAINT pk_ds PRIMARY KEY (doctor_profile_id, specialization_id),
    CONSTRAINT fk_ds_doctor FOREIGN KEY (doctor_profile_id) REFERENCES doctor_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_ds_spec FOREIGN KEY (specialization_id) REFERENCES specialization(id) ON DELETE CASCADE
);

-- 5. Data Migration (Ensure no data loss)

-- Link all hospitals to 'General' department
INSERT INTO hospital_departments (hospital_id, department_id)
SELECT h.id, d.id
FROM hospital h
CROSS JOIN department d
WHERE d.name = 'General';

-- Link all user_contracts to 'General' department
INSERT INTO user_contract_department (user_contract_id, department_id)
SELECT uc.id, d.id
FROM user_contract uc
CROSS JOIN department d
WHERE d.name = 'General';

-- Link all doctors to 'General Medicine' specialization
INSERT INTO doctor_specialization (doctor_profile_id, specialization_id)
SELECT dp.id, s.id
FROM doctor_profile dp
CROSS JOIN specialization s
WHERE s.name = 'General Medicine';

-- 6. Cleanup deprecated columns
ALTER TABLE user_contract DROP COLUMN department_name;
ALTER TABLE doctor_profile DROP COLUMN specialization;

-- 7. Add new columns to profile tables
ALTER TABLE doctor_profile ADD COLUMN IF NOT EXISTS registration_number varchar(100);
ALTER TABLE doctor_profile ADD COLUMN IF NOT EXISTS experience_years int4;
ALTER TABLE doctor_profile ADD COLUMN IF NOT EXISTS awards_json varchar(MAX) DEFAULT '[]';
ALTER TABLE doctor_profile ADD COLUMN IF NOT EXISTS languages_json varchar(MAX) DEFAULT '[]';
ALTER TABLE doctor_profile ADD COLUMN IF NOT EXISTS signature_url varchar(255);
ALTER TABLE doctor_profile ADD COLUMN IF NOT EXISTS consultation_fee numeric(19, 2);
ALTER TABLE doctor_profile ADD COLUMN IF NOT EXISTS metadata_json varchar(MAX) DEFAULT '{}';

-- receptionist_profile refactor
ALTER TABLE receptionist_profile ALTER COLUMN languages_spoken RENAME TO languages_spoken_json;
ALTER TABLE receptionist_profile DROP COLUMN IF EXISTS skills_json;

-- 8. Add missing indexes
CREATE INDEX IF NOT EXISTS idx_user_contract_user ON user_contract (user_id);
CREATE INDEX IF NOT EXISTS idx_user_contract_hospital ON user_contract (hospital_id);
CREATE INDEX IF NOT EXISTS idx_ucd_dept ON user_contract_department (department_id);
CREATE INDEX IF NOT EXISTS idx_ds_spec ON doctor_specialization (specialization_id);
