-- 1. Core Identity & Organizations
CREATE TABLE hospital (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    code varchar(50) UNIQUE NOT NULL,
    address varchar(1000) NULL,
    city varchar(100),
    state varchar(100),
    pincode varchar(20),
    status varchar(20) NOT NULL,
    gstin varchar(15) UNIQUE,
    registration_no varchar(100),
    settings varchar(MAX) DEFAULT '{}', -- H2 handles JSON as string/clob in Postgres mode
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE TABLE user_account (
    id uuid PRIMARY KEY,
    username varchar(100) UNIQUE NOT NULL,
    email varchar(255) UNIQUE,
    password_hash varchar(255) NOT NULL,
    full_name varchar(255) NULL,
    gender varchar(20) NULL,
    dob date NULL,
    phone varchar(20) NULL,
    is_active bool DEFAULT true NOT NULL,
    deleted bool DEFAULT false NOT NULL,
    is_superuser bool DEFAULT false NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE TABLE "role" (
    id uuid PRIMARY KEY,
    code varchar(50) UNIQUE NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(255) NULL,
    is_system bool NOT NULL,
    is_active bool NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE TABLE user_role_assignment (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    hospital_id uuid, -- NULL for global roles
    role_id uuid NOT NULL,
    department_name varchar(100) NULL,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_assign_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_assign_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_assign_role FOREIGN KEY (role_id) REFERENCES "role"(id)
);

-- 2. Staff Profiles
CREATE TABLE doctor_profile (
    id uuid PRIMARY KEY,
    specialization varchar(255) NOT NULL,
    license_number varchar(50) UNIQUE,
    education_json varchar(MAX),
    bio text,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_doctor_profile_user FOREIGN KEY (id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE TABLE nurse_profile (
    id uuid PRIMARY KEY,
    nursing_license_no varchar(50) UNIQUE NULL,
    certification_level varchar(50) NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_nurse_profile_user FOREIGN KEY (id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE TABLE pharmacist_profile (
    id uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    pci_reg_no varchar(50) UNIQUE NOT NULL,
    qualification varchar(100),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab_tech_profile (
    id uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    specialty varchar(100),
    license_no varchar(50),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE receptionist_profile (
    id uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    languages_spoken varchar(MAX),
    skills_json varchar(MAX),
    is_insurance_specialist bool DEFAULT false,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Hospital Details
CREATE TABLE admin_hospital_details (
    id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    office_location varchar(255),
    is_department_head bool DEFAULT false,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctor_hospital_details (
    id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    consultation_fee numeric(12, 2) DEFAULT 0.00,
    follow_up_days int4 DEFAULT 0,
    commission_type varchar(20),
    commission_value numeric(12, 2),
    is_visiting_consultant bool DEFAULT false,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE nurse_hospital_details (
    id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    floor_location varchar(255),
    shift_type varchar(50), -- e.g., DAY, NIGHT, ROTATIONAL
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pharmacist_hospital_details (
    id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    counter_location varchar(255),
    pharmacy_wing varchar(100),
    pharmacy_license_no varchar(50),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE receptionist_hospital_details (
    id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    desk_location varchar(255),
    emergency_contact_number varchar(20),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Clinical & Patients
CREATE TABLE patient (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    full_name varchar(255) NOT NULL,
    gender varchar(20) NULL,
    patient_code varchar(50) UNIQUE,
    dob date NULL,
    phone varchar(20) NULL,
    email varchar(255),
    address varchar(1000) NULL,
    medical_history text,
    blood_group varchar(10),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_pat_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

CREATE TABLE appointment (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    doctor_id uuid NOT NULL,
    appointment_time timestamp NOT NULL,
    token_number integer,
    session_period varchar(20),
    status varchar(30) NOT NULL,
    notes varchar(1000) NULL,
    chief_complaints text,
    diagnosis text,
    clinical_notes text,
    icd_codes varchar(MAX) DEFAULT '[]',
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_apt_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_apt_pat FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_apt_dr FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id)
);

CREATE TABLE vitals (
    id UUID PRIMARY KEY,
    systolic INTEGER,
    diastolic INTEGER,
    heart_rate INTEGER,
    temperature DECIMAL(5,2),
    temperature_unit VARCHAR(10) DEFAULT 'CELSIUS',
    weight DECIMAL(5,2),
    weight_unit VARCHAR(10) DEFAULT 'KG',
    oxygen_level NUMERIC(5, 2),
    blood_pressure VARCHAR(20),
    CONSTRAINT fk_vitals_appointment FOREIGN KEY (id) REFERENCES appointment(id) ON DELETE CASCADE
);

CREATE TABLE prescription (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    CONSTRAINT fk_prescription_appointment FOREIGN KEY (id) REFERENCES appointment(id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id)
);

-- 5. Pharmacy & Inventory
CREATE TABLE medicine (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    composition varchar(255) NULL,
    manufacturer varchar(255) NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_med_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

CREATE TABLE prescription_item (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL,
    medicine_id UUID NOT NULL,
    dosage VARCHAR(255) NOT NULL,
    duration VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_prei_pre FOREIGN KEY (prescription_id) REFERENCES prescription(id) ON DELETE CASCADE,
    CONSTRAINT fk_prei_med FOREIGN KEY (medicine_id) REFERENCES medicine(id)
);

CREATE TABLE item (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    category varchar(100) NULL,
    unit varchar(50) NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_item_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

CREATE TABLE inventory_stock (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    item_id uuid NOT NULL,
    quantity numeric(12, 2) NOT NULL,
    last_updated timestamp NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_invs_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_invs_itm FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT uq_invs_hosp_itm UNIQUE (hospital_id, item_id)
);

CREATE TABLE pharmacy_stock (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    medicine_id uuid NOT NULL,
    batch_number varchar(255) NOT NULL,
    expiry_date date NULL,
    quantity int4 NOT NULL,
    price numeric(12, 2) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_phrs_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_phrs_med FOREIGN KEY (medicine_id) REFERENCES medicine(id)
);

CREATE TABLE inventory_transaction (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    item_id uuid NOT NULL,
    type varchar(50) NOT NULL,
    quantity numeric(12, 2) NOT NULL,
    reference varchar(255),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_invt_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_invt_itm FOREIGN KEY (item_id) REFERENCES item(id)
);

-- 6. Records & Billing
CREATE TABLE billing (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    appointment_id UUID REFERENCES appointment(id),
    source_type varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    total_amount numeric(12, 2) NOT NULL,
    discount numeric(12, 2) DEFAULT 0.00,
    payment_method varchar(50),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_bill_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_bill_pat FOREIGN KEY (patient_id) REFERENCES patient(id)
);

CREATE TABLE billing_item (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    billing_id uuid NOT NULL,
    item_name varchar(255) NOT NULL,
    amount numeric(10, 2) NOT NULL,
    quantity int4 NOT NULL,
    subtotal numeric(13, 2) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_billi_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_billi_bill FOREIGN KEY (billing_id) REFERENCES billing(id)
);

-- 7. Audit & Support
CREATE TABLE audit_log (
    id uuid PRIMARY KEY,
    hospital_id uuid NULL,
    entity_name varchar(100) NOT NULL,
    entity_id uuid NULL,
    action varchar(50) NOT NULL,
    performed_by uuid NULL,
    details varchar(MAX),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_audit_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

CREATE TABLE doctor_schedules (
    id uuid PRIMARY KEY,
    assignment_id uuid NOT NULL REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    day_of_week int2 CHECK (day_of_week BETWEEN 1 AND 7),
    start_time time NOT NULL,
    end_time time NOT NULL,
    slot_duration_minutes int4 DEFAULT 15,
    max_patients int4
);

CREATE TABLE doctor_availability_exception (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    hospital_id UUID NOT NULL,
    exception_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'FULL_DAY_OFF', 'MODIFIED', 'ADDED'
    start_time TIME,
    end_time TIME,
    mode VARCHAR(10),          -- override mode if needed
    slot_duration_minutes INT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ex_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_ex_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id) ON DELETE CASCADE
);

CREATE TABLE vital_range_configs (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    vital_type VARCHAR(50) NOT NULL,
    min_value NUMERIC(10, 2),
    max_value NUMERIC(10, 2),
    unit VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vital_hospital UNIQUE (hospital_id, vital_type),
    CONSTRAINT fk_vrc_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

-- Indices
CREATE INDEX idx_assignment_user_hosp ON user_role_assignment (user_id, hospital_id);
CREATE INDEX idx_pat_hosp ON patient (hospital_id);
CREATE INDEX idx_apt_time ON appointment (hospital_id, appointment_time);
CREATE UNIQUE INDEX uq_appointment_doctor_date_token ON appointment (doctor_id, appointment_time, token_number);
CREATE INDEX idx_invs_hosp ON inventory_stock (hospital_id);
CREATE INDEX idx_invt_hosp ON inventory_transaction (hospital_id);
CREATE INDEX idx_invt_item ON inventory_transaction (item_id);
CREATE INDEX idx_billi_hospital ON billing_item (hospital_id);
CREATE INDEX idx_dr_schedule_assignment ON doctor_schedules(assignment_id, day_of_week);
CREATE INDEX idx_audit_log_hosp_time ON audit_log (hospital_id, created_at);
CREATE INDEX idx_dr_avail_ex_lookup ON doctor_availability_exception (doctor_id, hospital_id, exception_date);
CREATE INDEX idx_billing_appointment ON billing (appointment_id);
CREATE INDEX idx_prescription_patient ON prescription (patient_id);
CREATE INDEX idx_prescription_doctor ON prescription (doctor_id);
CREATE INDEX idx_prescription_item_prescription ON prescription_item (prescription_id);

-- Default Vital Range Configs Seed
INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'BODY_TEMPERATURE', 95, 108, 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hospital h;

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'SYSTOLIC_BP', 70, 220, 'mmHg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hospital h;

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'DIASTOLIC_BP', 40, 140, 'mmHg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hospital h;

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'OXYGEN_LEVEL', 50, 100, '%', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hospital h;

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'WEIGHT', 1, 300, 'kg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hospital h;

INSERT INTO vital_range_configs (id, hospital_id, vital_type, min_value, max_value, unit, created_at, updated_at)
SELECT RANDOM_UUID(), h.id, 'HEART_RATE', 30, 250, 'bpm', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hospital h;
