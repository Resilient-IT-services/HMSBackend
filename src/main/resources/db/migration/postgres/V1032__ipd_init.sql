-- V1028__ipd_init.sql
CREATE SCHEMA IF NOT EXISTS ipd;

-- Ward Table
CREATE TABLE ipd.ward (
                          id UUID PRIMARY KEY,
                          hospital_id UUID NOT NULL,
                          ward_code VARCHAR(50) NOT NULL,
                          ward_name VARCHAR(255) NOT NULL,
                          ward_type VARCHAR(50) NOT NULL,
                          total_beds INTEGER NOT NULL DEFAULT 0,
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          remarks TEXT,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT uk_ward_hospital_code UNIQUE (hospital_id, ward_code)
);

-- Bed Table
CREATE TABLE ipd.bed (
                         id UUID PRIMARY KEY,
                         hospital_id UUID NOT NULL,
                         ward_id UUID NOT NULL,
                         bed_number VARCHAR(50) NOT NULL,
                         status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
                         active BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_bed_ward FOREIGN KEY (ward_id) REFERENCES ipd.ward(id),
                         CONSTRAINT uk_bed_hospital_ward_number UNIQUE (hospital_id, ward_id, bed_number)
);

-- Admission Table
CREATE TABLE ipd.admission (
                               id UUID PRIMARY KEY,
                               hospital_id UUID NOT NULL,
                               admission_number VARCHAR(50) NOT NULL,
                               patient_id UUID NOT NULL,
                               doctor_id UUID NOT NULL,
                               bed_id UUID NOT NULL,
                               admission_date_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               status VARCHAR(50) NOT NULL DEFAULT 'ADMITTED',
                               admission_reason TEXT,
                               remarks TEXT,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT uk_admission_hospital_number UNIQUE (hospital_id, admission_number),
                               CONSTRAINT fk_admission_patient FOREIGN KEY (patient_id) REFERENCES patient.patient(id),
                               CONSTRAINT fk_admission_doctor FOREIGN KEY (doctor_id) REFERENCES iam.user_account(id),
                               CONSTRAINT fk_admission_bed FOREIGN KEY (bed_id) REFERENCES ipd.bed(id)
);

-- Sequence for Admission Numbers
CREATE SEQUENCE IF NOT EXISTS ipd.admission_number_seq START 1;
