-- 1. Core Identity & Organizations
CREATE TABLE hospital (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    code varchar(50) UNIQUE NOT NULL,
    address varchar(1000) NULL,
    status varchar(20) NOT NULL,
    gstin varchar(15) UNIQUE,
    registration_no varchar(100),
    settings jsonb DEFAULT '{}',
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE TABLE user_account (
    id uuid PRIMARY KEY,
    username varchar(100) UNIQUE NOT NULL,
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
CREATE TABLE doctor (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE,
    specialization varchar(255) NOT NULL,
    license_number varchar(50) UNIQUE,
    education_json jsonb,
    bio text,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE nurse (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE,
    department varchar(255) NOT NULL,
    nursing_license_no varchar(50) UNIQUE NULL,
    certification_level varchar(50) NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_nurse_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE pharmacist_profile (
    user_id uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    pci_reg_no varchar(50) UNIQUE NOT NULL,
    qualification varchar(100)
);

CREATE TABLE lab_tech_profile (
    user_id uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    specialty varchar(100),
    license_no varchar(50)
);

CREATE TABLE receptionist_profile (
    user_id uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    languages_spoken jsonb,
    skills_json jsonb,
    is_insurance_specialist bool DEFAULT false
);

CREATE TABLE doctor_hospital_details (
    assignment_id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    consultation_fee numeric(12, 2) DEFAULT 0.00,
    follow_up_days int4 DEFAULT 0,
    commission_type varchar(20),
    commission_value numeric(12, 2),
    is_visiting_consultant bool DEFAULT false,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Clinical & Patients
CREATE TABLE patient (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    full_name varchar(255) NOT NULL,
    gender varchar(20) NULL,
    patient_code varchar(50) UNIQUE,
    dob date NULL,
    phone varchar(20) NULL,
    address varchar(1000) NULL,
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
    status varchar(30) NOT NULL,
    notes varchar(1000) NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_apt_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_apt_pat FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_apt_dr FOREIGN KEY (doctor_id) REFERENCES doctor(id)
);

-- 4. Pharmacy & Inventory
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

-- 5. Records & Billing
CREATE TABLE prescription (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    doctor_id uuid NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_pre_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_pre_pat FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_pre_dr FOREIGN KEY (doctor_id) REFERENCES doctor(id)
);

CREATE TABLE prescription_item (
    id uuid PRIMARY KEY,
    prescription_id uuid NOT NULL,
    medicine_id uuid NOT NULL,
    hospital_id uuid NOT NULL,
    dosage varchar(255) NOT NULL,
    duration varchar(255) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_prei_pre FOREIGN KEY (prescription_id) REFERENCES prescription(id),
    CONSTRAINT fk_prei_med FOREIGN KEY (medicine_id) REFERENCES medicine(id),
    CONSTRAINT fk_prei_hosp FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

CREATE TABLE billing (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    source_type varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    total_amount numeric(12, 2) NOT NULL,
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

-- 6. Audit & Support
CREATE TABLE audit_log (
    id uuid PRIMARY KEY,
    hospital_id uuid NULL,
    entity_name varchar(100) NOT NULL,
    entity_id uuid NULL,
    action varchar(50) NOT NULL,
    performed_by uuid NULL,
    details jsonb NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_audit_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

CREATE TABLE doctor_schedules (
    id uuid PRIMARY KEY,
    assignment_id uuid NOT NULL REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    day_of_week int2 CHECK (day_of_week BETWEEN 0 AND 6),
    start_time time NOT NULL,
    end_time time NOT NULL,
    slot_duration_minutes int4 DEFAULT 15,
    max_patients int4
);

CREATE TABLE staff_absences (
    id uuid PRIMARY KEY,
    assignment_id uuid NOT NULL REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    absence_date date NOT NULL,
    reason text
);

-- Indices
CREATE INDEX idx_assignment_user_hosp ON user_role_assignment (user_id, hospital_id);
CREATE INDEX idx_pat_hosp ON patient (hospital_id);
CREATE INDEX idx_apt_time ON appointment (hospital_id, appointment_time);
CREATE INDEX idx_invs_hosp ON inventory_stock (hospital_id);
CREATE INDEX idx_invt_hosp ON inventory_transaction (hospital_id);
CREATE INDEX idx_invt_item ON inventory_transaction (item_id);
CREATE INDEX idx_billi_hospital ON billing_item (hospital_id);
CREATE INDEX idx_dr_schedule_assignment ON doctor_schedules(assignment_id, day_of_week);
CREATE INDEX idx_audit_log_hosp_time ON audit_log (hospital_id, created_at);
