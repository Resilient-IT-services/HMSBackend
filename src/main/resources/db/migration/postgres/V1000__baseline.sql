-- public.department definition

-- Drop table

-- DROP TABLE public.department;

CREATE TABLE public.department ( id uuid NOT NULL, "name" varchar(255) NOT NULL, description text NULL, is_active bool DEFAULT true NOT NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT department_name_key UNIQUE (name), CONSTRAINT department_pkey PRIMARY KEY (id));

-- Permissions

ALTER TABLE public.department OWNER TO postgres;
GRANT ALL ON TABLE public.department TO postgres;


-- public.flyway_schema_history definition

-- Drop table

-- DROP TABLE public.flyway_schema_history;

CREATE TABLE public.flyway_schema_history ( installed_rank int4 NOT NULL, "version" varchar(50) NULL, description varchar(200) NOT NULL, "type" varchar(20) NOT NULL, script varchar(1000) NOT NULL, checksum int4 NULL, installed_by varchar(100) NOT NULL, installed_on timestamp DEFAULT now() NOT NULL, execution_time int4 NOT NULL, success bool NOT NULL, CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank));
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

-- Permissions

ALTER TABLE public.flyway_schema_history OWNER TO postgres;
GRANT ALL ON TABLE public.flyway_schema_history TO postgres;


-- public.hospital definition

-- Drop table

-- DROP TABLE public.hospital;

CREATE TABLE public.hospital ( id uuid NOT NULL, "name" varchar(255) NOT NULL, code varchar(50) NOT NULL, address varchar(1000) NULL, city varchar(100) NULL, state varchar(100) NULL, pincode varchar(20) NULL, status varchar(20) NOT NULL, gstin varchar(15) NULL, registration_no varchar(100) NULL, settings jsonb DEFAULT '{}'::jsonb NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, deleted bool DEFAULT false NOT NULL, timezone varchar(50) DEFAULT 'Asia/Kolkata'::character varying NOT NULL, CONSTRAINT chk_hospital_status CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('SUSPENDED'::character varying)::text, ('ONBOARDING'::character varying)::text]))), CONSTRAINT hospital_code_key UNIQUE (code), CONSTRAINT hospital_gstin_key UNIQUE (gstin), CONSTRAINT hospital_pkey PRIMARY KEY (id));

-- Permissions

ALTER TABLE public.hospital OWNER TO postgres;
GRANT ALL ON TABLE public.hospital TO postgres;


-- public.medicine definition

-- Drop table

-- DROP TABLE public.medicine;

CREATE TABLE public.medicine ( id uuid NOT NULL, "name" varchar(255) NOT NULL, composition varchar(255) NULL, manufacturer varchar(255) NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, generic_name varchar(255) NULL, storage_temperature varchar(50) NULL, strength varchar(100) NULL, form varchar(100) NULL, schedule_type varchar(100) NULL, deleted bool DEFAULT false NOT NULL, threshold_quantity int4 DEFAULT 10 NOT NULL, hsn_code varchar(50) NULL, tax_percentage numeric(5, 2) DEFAULT 0 NULL, CONSTRAINT chk_medicine_form CHECK (((form)::text = ANY (ARRAY[('TABLET'::character varying)::text, ('SYRUP'::character varying)::text, ('INJECTION'::character varying)::text, ('CAPSULE'::character varying)::text, ('OINTMENT'::character varying)::text, ('CREAM'::character varying)::text, ('DROPS'::character varying)::text, ('INHALER'::character varying)::text]))), CONSTRAINT chk_medicine_schedule_type CHECK (((schedule_type)::text = ANY (ARRAY[('SCHEDULE_H'::character varying)::text, ('SCHEDULE_G'::character varying)::text, ('SCHEDULE_X'::character varying)::text, ('OTC'::character varying)::text]))), CONSTRAINT medicine_pkey PRIMARY KEY (id));
CREATE INDEX idx_medicine_composition ON public.medicine USING btree (lower((composition)::text));
CREATE INDEX idx_medicine_generic_name ON public.medicine USING btree (lower((generic_name)::text));
CREATE INDEX idx_medicine_name ON public.medicine USING btree (lower((name)::text));
CREATE INDEX idx_medicine_search_trgm ON public.medicine USING gin (((((((COALESCE(name, ''::character varying))::text || ' '::text) || (COALESCE(generic_name, ''::character varying))::text) || ' '::text) || (COALESCE(composition, ''::character varying))::text)) gin_trgm_ops);

-- Permissions

ALTER TABLE public.medicine OWNER TO postgres;
GRANT ALL ON TABLE public.medicine TO postgres;


-- public.outbox_event definition

-- Drop table

-- DROP TABLE public.outbox_event;

CREATE TABLE public.outbox_event ( id uuid NOT NULL, aggregate_type varchar(255) NOT NULL, aggregate_id uuid NOT NULL, event_type varchar(255) NOT NULL, payload jsonb NOT NULL, created_at timestamp NOT NULL, processed_at timestamp NULL, CONSTRAINT outbox_event_pkey PRIMARY KEY (id));
CREATE INDEX idx_outbox_unprocessed ON public.outbox_event USING btree (created_at) WHERE (processed_at IS NULL);

-- Permissions

ALTER TABLE public.outbox_event OWNER TO postgres;
GRANT ALL ON TABLE public.outbox_event TO postgres;


-- public."role" definition

-- Drop table

-- DROP TABLE public."role";

CREATE TABLE public."role" ( id uuid NOT NULL, code varchar(50) NOT NULL, "name" varchar(100) NOT NULL, description varchar(255) NULL, is_system bool NOT NULL, is_active bool NOT NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, CONSTRAINT chk_role_code CHECK (((code)::text = ANY (ARRAY[('ADMIN'::character varying)::text, ('DOCTOR'::character varying)::text, ('NURSE'::character varying)::text, ('RECEPTIONIST'::character varying)::text, ('PHARMACIST'::character varying)::text]))), CONSTRAINT role_code_key UNIQUE (code), CONSTRAINT role_pkey PRIMARY KEY (id));

-- Permissions

ALTER TABLE public."role" OWNER TO postgres;
GRANT ALL ON TABLE public."role" TO postgres;


-- public.specialization definition

-- Drop table

-- DROP TABLE public.specialization;

CREATE TABLE public.specialization ( id uuid NOT NULL, "name" varchar(255) NOT NULL, description text NULL, is_active bool DEFAULT true NOT NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT specialization_name_key UNIQUE (name), CONSTRAINT specialization_pkey PRIMARY KEY (id));

-- Permissions

ALTER TABLE public.specialization OWNER TO postgres;
GRANT ALL ON TABLE public.specialization TO postgres;


-- public.user_account definition

-- Drop table

-- DROP TABLE public.user_account;

CREATE TABLE public.user_account ( id uuid NOT NULL, username varchar(100) NOT NULL, email varchar(255) NULL, password_hash varchar(255) NOT NULL, full_name varchar(255) NULL, gender varchar(20) NULL, dob date NULL, phone varchar(20) NULL, is_active bool DEFAULT true NOT NULL, deleted bool DEFAULT false NOT NULL, is_superuser bool DEFAULT false NOT NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, CONSTRAINT chk_user_account_gender CHECK (((gender)::text = ANY (ARRAY[('MALE'::character varying)::text, ('FEMALE'::character varying)::text, ('OTHER'::character varying)::text]))), CONSTRAINT user_account_email_key UNIQUE (email), CONSTRAINT user_account_pkey PRIMARY KEY (id), CONSTRAINT user_account_username_key UNIQUE (username));

-- Permissions

ALTER TABLE public.user_account OWNER TO postgres;
GRANT ALL ON TABLE public.user_account TO postgres;


-- public.vital_range_configs definition

-- Drop table

-- DROP TABLE public.vital_range_configs;

CREATE TABLE public.vital_range_configs ( id uuid NOT NULL, vital_type varchar(50) NOT NULL, min_value numeric(10, 2) NULL, max_value numeric(10, 2) NULL, created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL, updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL, CONSTRAINT chk_vital_range_configs_vital_type CHECK (((vital_type)::text = ANY (ARRAY[('BODY_TEMPERATURE'::character varying)::text, ('SYSTOLIC_BP'::character varying)::text, ('DIASTOLIC_BP'::character varying)::text, ('OXYGEN_LEVEL'::character varying)::text, ('WEIGHT'::character varying)::text, ('HEART_RATE'::character varying)::text]))), CONSTRAINT uk_vital_type UNIQUE (vital_type), CONSTRAINT vital_range_configs_pkey PRIMARY KEY (id));

-- Permissions

ALTER TABLE public.vital_range_configs OWNER TO postgres;
GRANT ALL ON TABLE public.vital_range_configs TO postgres;


-- public.audit_log definition

-- Drop table

-- DROP TABLE public.audit_log;

CREATE TABLE public.audit_log ( id uuid NOT NULL, hospital_id uuid NULL, entity_name varchar(100) NOT NULL, entity_id uuid NULL, "action" varchar(50) NOT NULL, performed_by uuid NULL, details jsonb NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, CONSTRAINT audit_log_pkey PRIMARY KEY (id), CONSTRAINT chk_audit_log_action CHECK (((action)::text = ANY (ARRAY[('CREATE'::character varying)::text, ('UPDATE'::character varying)::text, ('DELETE'::character varying)::text, ('LOGIN'::character varying)::text, ('LOGOUT'::character varying)::text, ('UPDATE_STATUS'::character varying)::text]))), CONSTRAINT chk_audit_log_entity_name CHECK (((entity_name)::text = ANY (ARRAY[('PATIENT'::character varying)::text, ('APPOINTMENT'::character varying)::text, ('INVOICE'::character varying)::text, ('PAYMENT'::character varying)::text, ('PRESCRIPTION'::character varying)::text, ('MEDICINE'::character varying)::text, ('HOSPITAL'::character varying)::text, ('USER_ACCOUNT'::character varying)::text, ('USER_CONTRACT'::character varying)::text]))), CONSTRAINT fk_audit_hospital FOREIGN KEY (hospital_id) REFERENCES public.hospital(id));
CREATE INDEX idx_audit_log_entity ON public.audit_log USING btree (entity_name, entity_id);
CREATE INDEX idx_audit_log_hosp_time ON public.audit_log USING btree (hospital_id, created_at);

-- Permissions

ALTER TABLE public.audit_log OWNER TO postgres;
GRANT ALL ON TABLE public.audit_log TO postgres;


-- public.doctor_profile definition

-- Drop table

-- DROP TABLE public.doctor_profile;

CREATE TABLE public.doctor_profile ( id uuid NOT NULL, license_number varchar(50) NULL, education_json jsonb NULL, bio text NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, registration_number varchar(100) NULL, experience_years int4 NULL, awards_json jsonb DEFAULT '[]'::jsonb NULL, languages_json jsonb DEFAULT '[]'::jsonb NULL, signature_url varchar(255) NULL, consultation_fee numeric(19, 2) NULL, metadata_json jsonb DEFAULT '{}'::jsonb NULL, CONSTRAINT doctor_profile_license_number_key UNIQUE (license_number), CONSTRAINT doctor_profile_pkey PRIMARY KEY (id), CONSTRAINT fk_doctor_profile_user FOREIGN KEY (id) REFERENCES public.user_account(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.doctor_profile OWNER TO postgres;
GRANT ALL ON TABLE public.doctor_profile TO postgres;


-- public.doctor_specialization definition

-- Drop table

-- DROP TABLE public.doctor_specialization;

CREATE TABLE public.doctor_specialization ( doctor_profile_id uuid NOT NULL, specialization_id uuid NOT NULL, CONSTRAINT pk_ds PRIMARY KEY (doctor_profile_id, specialization_id), CONSTRAINT fk_ds_doctor FOREIGN KEY (doctor_profile_id) REFERENCES public.doctor_profile(id) ON DELETE CASCADE, CONSTRAINT fk_ds_spec FOREIGN KEY (specialization_id) REFERENCES public.specialization(id) ON DELETE CASCADE);
CREATE INDEX idx_ds_spec ON public.doctor_specialization USING btree (specialization_id);

-- Permissions

ALTER TABLE public.doctor_specialization OWNER TO postgres;
GRANT ALL ON TABLE public.doctor_specialization TO postgres;


-- public.hospital_departments definition

-- Drop table

-- DROP TABLE public.hospital_departments;

CREATE TABLE public.hospital_departments ( hospital_id uuid NOT NULL, department_id uuid NOT NULL, CONSTRAINT pk_hd PRIMARY KEY (hospital_id, department_id), CONSTRAINT fk_hd_dept FOREIGN KEY (department_id) REFERENCES public.department(id) ON DELETE CASCADE, CONSTRAINT fk_hd_hospital FOREIGN KEY (hospital_id) REFERENCES public.hospital(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.hospital_departments OWNER TO postgres;
GRANT ALL ON TABLE public.hospital_departments TO postgres;


-- public.hospital_medicine_catalog definition

-- Drop table

-- DROP TABLE public.hospital_medicine_catalog;

CREATE TABLE public.hospital_medicine_catalog ( id uuid NOT NULL, hospital_id uuid NOT NULL, medicine_id uuid NOT NULL, reorder_level int4 DEFAULT 0 NULL, base_selling_price numeric(12, 2) NULL, tax_percentage numeric(5, 2) NULL, is_active bool DEFAULT true NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, CONSTRAINT hospital_medicine_catalog_pkey PRIMARY KEY (id), CONSTRAINT uk_hosp_med UNIQUE (hospital_id, medicine_id), CONSTRAINT fk_hmc_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_hmc_med FOREIGN KEY (medicine_id) REFERENCES public.medicine(id));
CREATE INDEX idx_hmc_hosp_med ON public.hospital_medicine_catalog USING btree (hospital_id, medicine_id);

-- Permissions

ALTER TABLE public.hospital_medicine_catalog OWNER TO postgres;
GRANT ALL ON TABLE public.hospital_medicine_catalog TO postgres;


-- public.hsn_master definition

-- Drop table

-- DROP TABLE public.hsn_master;

CREATE TABLE public.hsn_master ( id uuid NOT NULL, hospital_id uuid NOT NULL, hsn_code varchar(50) NOT NULL, description varchar(255) NULL, tax_percentage numeric(5, 2) NOT NULL, is_active bool DEFAULT true NOT NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT hsn_master_pkey PRIMARY KEY (id), CONSTRAINT hsn_master_hospital_id_fkey FOREIGN KEY (hospital_id) REFERENCES public.hospital(id));
CREATE INDEX idx_hsn_master_hosp_code ON public.hsn_master USING btree (hospital_id, hsn_code);

-- Permissions

ALTER TABLE public.hsn_master OWNER TO postgres;
GRANT ALL ON TABLE public.hsn_master TO postgres;


-- public.item definition

-- Drop table

-- DROP TABLE public.item;

CREATE TABLE public.item ( id uuid NOT NULL, hospital_id uuid NOT NULL, "name" varchar(255) NOT NULL, category varchar(100) NULL, unit varchar(50) NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, deleted bool DEFAULT false NOT NULL, CONSTRAINT chk_item_unit CHECK (((unit)::text = ANY (ARRAY[('PIECE'::character varying)::text, ('BOX'::character varying)::text, ('STRIP'::character varying)::text, ('BOTTLE'::character varying)::text, ('PACK'::character varying)::text, ('VIAL'::character varying)::text, ('AMPULE'::character varying)::text]))), CONSTRAINT item_pkey PRIMARY KEY (id), CONSTRAINT fk_item_hospital FOREIGN KEY (hospital_id) REFERENCES public.hospital(id));

-- Permissions

ALTER TABLE public.item OWNER TO postgres;
GRANT ALL ON TABLE public.item TO postgres;


-- public.lab_tech_profile definition

-- Drop table

-- DROP TABLE public.lab_tech_profile;

CREATE TABLE public.lab_tech_profile ( id uuid NOT NULL, specialty varchar(100) NULL, license_no varchar(50) NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT lab_tech_profile_pkey PRIMARY KEY (id), CONSTRAINT lab_tech_profile_id_fkey FOREIGN KEY (id) REFERENCES public.user_account(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.lab_tech_profile OWNER TO postgres;
GRANT ALL ON TABLE public.lab_tech_profile TO postgres;


-- public.nurse_profile definition

-- Drop table

-- DROP TABLE public.nurse_profile;

CREATE TABLE public.nurse_profile ( id uuid NOT NULL, nursing_license_no varchar(50) NULL, certification_level varchar(50) NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, CONSTRAINT chk_nurse_profile_certification_level CHECK (((certification_level)::text = ANY (ARRAY[('RN'::character varying)::text, ('LPN'::character varying)::text, ('NP'::character varying)::text, ('CNA'::character varying)::text]))), CONSTRAINT nurse_profile_nursing_license_no_key UNIQUE (nursing_license_no), CONSTRAINT nurse_profile_pkey PRIMARY KEY (id), CONSTRAINT fk_nurse_profile_user FOREIGN KEY (id) REFERENCES public.user_account(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.nurse_profile OWNER TO postgres;
GRANT ALL ON TABLE public.nurse_profile TO postgres;


-- public.patient definition

-- Drop table

-- DROP TABLE public.patient;

CREATE TABLE public.patient ( id uuid NOT NULL, hospital_id uuid NOT NULL, full_name varchar(255) NOT NULL, gender varchar(20) NULL, patient_code varchar(50) NULL, dob date NULL, phone varchar(20) NULL, email varchar(255) NULL, address varchar(1000) NULL, medical_history text NULL, blood_group varchar(10) NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, deleted bool DEFAULT false NOT NULL, CONSTRAINT chk_patient_blood_group CHECK (((blood_group)::text = ANY (ARRAY[('A_POSITIVE'::character varying)::text, ('A_NEGATIVE'::character varying)::text, ('B_POSITIVE'::character varying)::text, ('B_NEGATIVE'::character varying)::text, ('AB_POSITIVE'::character varying)::text, ('AB_NEGATIVE'::character varying)::text, ('O_POSITIVE'::character varying)::text, ('O_NEGATIVE'::character varying)::text]))), CONSTRAINT chk_patient_gender CHECK (((gender)::text = ANY (ARRAY[('MALE'::character varying)::text, ('FEMALE'::character varying)::text, ('OTHER'::character varying)::text]))), CONSTRAINT patient_patient_code_key UNIQUE (patient_code), CONSTRAINT patient_pkey PRIMARY KEY (id), CONSTRAINT fk_pat_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id));
CREATE INDEX idx_pat_hosp ON public.patient USING btree (hospital_id);

-- Permissions

ALTER TABLE public.patient OWNER TO postgres;
GRANT ALL ON TABLE public.patient TO postgres;


-- public.patient_visit definition

-- Drop table

-- DROP TABLE public.patient_visit;

CREATE TABLE public.patient_visit ( id uuid NOT NULL, hospital_id uuid NOT NULL, patient_id uuid NOT NULL, visit_number varchar(50) NOT NULL, visit_type varchar(20) NOT NULL, status varchar(20) NOT NULL, started_at timestamptz NOT NULL, ended_at timestamptz NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT chk_patient_visit_status CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('DISCHARGED'::character varying)::text, ('CANCELLED'::character varying)::text]))), CONSTRAINT chk_patient_visit_type CHECK (((visit_type)::text = ANY (ARRAY[('OPD'::character varying)::text, ('IPD'::character varying)::text, ('EMERGENCY'::character varying)::text, ('WALKIN'::character varying)::text]))), CONSTRAINT patient_visit_pkey PRIMARY KEY (id), CONSTRAINT uk_visit_number UNIQUE (hospital_id, visit_number), CONSTRAINT patient_visit_hospital_id_fkey FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT patient_visit_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient(id));
CREATE INDEX idx_visit_hospital_status ON public.patient_visit USING btree (hospital_id, status);
CREATE INDEX idx_visit_patient ON public.patient_visit USING btree (patient_id);

-- Permissions

ALTER TABLE public.patient_visit OWNER TO postgres;
GRANT ALL ON TABLE public.patient_visit TO postgres;


-- public.payment definition

-- Drop table

-- DROP TABLE public.payment;

CREATE TABLE public.payment ( id uuid NOT NULL, hospital_id uuid NOT NULL, patient_id uuid NULL, receipt_number varchar(50) NOT NULL, amount numeric(12, 2) NOT NULL, payment_mode varchar(20) NOT NULL, reference_number varchar(100) NULL, status varchar(20) NOT NULL, payment_date timestamptz NOT NULL, collected_by uuid NOT NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, visit_id uuid NULL, CONSTRAINT chk_payment_mode CHECK (((payment_mode)::text = ANY (ARRAY[('CASH'::character varying)::text, ('CARD'::character varying)::text, ('UPI'::character varying)::text, ('NEFT'::character varying)::text, ('INSURANCE'::character varying)::text]))), CONSTRAINT chk_payment_status CHECK (((status)::text = ANY (ARRAY[('SUCCESS'::character varying)::text, ('FAILED'::character varying)::text, ('REFUNDED'::character varying)::text]))), CONSTRAINT payment_pkey PRIMARY KEY (id), CONSTRAINT uk_receipt_number UNIQUE (hospital_id, receipt_number), CONSTRAINT payment_collected_by_fkey FOREIGN KEY (collected_by) REFERENCES public.user_account(id), CONSTRAINT payment_hospital_id_fkey FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT payment_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient(id), CONSTRAINT payment_visit_id_fkey FOREIGN KEY (visit_id) REFERENCES public.patient_visit(id));
CREATE INDEX idx_payment_visit ON public.payment USING btree (visit_id);

-- Permissions

ALTER TABLE public.payment OWNER TO postgres;
GRANT ALL ON TABLE public.payment TO postgres;


-- public.pharmacist_profile definition

-- Drop table

-- DROP TABLE public.pharmacist_profile;

CREATE TABLE public.pharmacist_profile ( id uuid NOT NULL, pci_reg_no varchar(50) NOT NULL, qualification varchar(100) NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT pharmacist_profile_pci_reg_no_key UNIQUE (pci_reg_no), CONSTRAINT pharmacist_profile_pkey PRIMARY KEY (id), CONSTRAINT pharmacist_profile_id_fkey FOREIGN KEY (id) REFERENCES public.user_account(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.pharmacist_profile OWNER TO postgres;
GRANT ALL ON TABLE public.pharmacist_profile TO postgres;


-- public.pharmacy_sale definition

-- Drop table

-- DROP TABLE public.pharmacy_sale;

CREATE TABLE public.pharmacy_sale ( id uuid NOT NULL, hospital_id uuid NOT NULL, patient_id uuid NULL, prescription_id uuid NULL, total_amount numeric(12, 2) DEFAULT 0 NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, visit_id uuid NULL, sale_type varchar(30) DEFAULT 'VISIT'::character varying NULL, external_doctor_name varchar(255) NULL, customer_name varchar(255) NULL, customer_phone varchar(20) NULL, invoice_id uuid NULL, sale_number varchar(50) NULL, status varchar(30) DEFAULT 'COMPLETED'::character varying NULL, doctor_id uuid NULL, notes text NULL, CONSTRAINT chk_pharmacy_sale_sale_type CHECK (((sale_type)::text = ANY (ARRAY[('VISIT'::character varying)::text, ('WALKIN'::character varying)::text]))), CONSTRAINT chk_pharmacy_sale_status CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('COMPLETED'::character varying)::text, ('CANCELLED'::character varying)::text]))), CONSTRAINT pharmacy_sale_pkey PRIMARY KEY (id), CONSTRAINT fk_pharmacy_sale_hospital FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT pharmacy_sale_visit_id_fkey FOREIGN KEY (visit_id) REFERENCES public.patient_visit(id));
CREATE INDEX idx_pharmacy_sale_visit ON public.pharmacy_sale USING btree (visit_id);
CREATE UNIQUE INDEX uq_pharmacy_sale_prescription ON public.pharmacy_sale USING btree (prescription_id) WHERE (prescription_id IS NOT NULL);

-- Permissions

ALTER TABLE public.pharmacy_sale OWNER TO postgres;
GRANT ALL ON TABLE public.pharmacy_sale TO postgres;


-- public.pharmacy_stock definition

-- Drop table

-- DROP TABLE public.pharmacy_stock;

CREATE TABLE public.pharmacy_stock ( id uuid NOT NULL, hospital_id uuid NOT NULL, medicine_id uuid NOT NULL, batch_number varchar(255) NOT NULL, expiry_date date NULL, quantity int4 NOT NULL, selling_price numeric(12, 2) NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, purchase_price numeric(12, 2) DEFAULT 0 NOT NULL, "version" int8 DEFAULT 0 NOT NULL, CONSTRAINT pharmacy_stock_pkey PRIMARY KEY (id), CONSTRAINT fk_phrs_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_phrs_med FOREIGN KEY (medicine_id) REFERENCES public.medicine(id));
CREATE INDEX idx_pharmacy_stock_fefo ON public.pharmacy_stock USING btree (hospital_id, medicine_id, expiry_date, quantity);
CREATE INDEX idx_pharmacy_stock_hospital_medicine ON public.pharmacy_stock USING btree (hospital_id, medicine_id);
CREATE UNIQUE INDEX uq_pharmacy_stock_batch ON public.pharmacy_stock USING btree (hospital_id, medicine_id, batch_number);

-- Permissions

ALTER TABLE public.pharmacy_stock OWNER TO postgres;
GRANT ALL ON TABLE public.pharmacy_stock TO postgres;


-- public.receptionist_profile definition

-- Drop table

-- DROP TABLE public.receptionist_profile;

CREATE TABLE public.receptionist_profile ( id uuid NOT NULL, languages_spoken_json jsonb NULL, is_insurance_specialist bool DEFAULT false NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT receptionist_profile_pkey PRIMARY KEY (id), CONSTRAINT receptionist_profile_id_fkey FOREIGN KEY (id) REFERENCES public.user_account(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.receptionist_profile OWNER TO postgres;
GRANT ALL ON TABLE public.receptionist_profile TO postgres;


-- public.user_contract definition

-- Drop table

-- DROP TABLE public.user_contract;

CREATE TABLE public.user_contract ( id uuid NOT NULL, user_id uuid NOT NULL, hospital_id uuid NULL, role_id uuid NOT NULL, is_active bool DEFAULT true NOT NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, CONSTRAINT user_contract_pkey PRIMARY KEY (id), CONSTRAINT fk_contract_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_contract_role FOREIGN KEY (role_id) REFERENCES public."role"(id), CONSTRAINT fk_contract_user FOREIGN KEY (user_id) REFERENCES public.user_account(id));
CREATE INDEX idx_assignment_user_hosp ON public.user_contract USING btree (user_id, hospital_id);
CREATE INDEX idx_user_contract_hospital ON public.user_contract USING btree (hospital_id);
CREATE INDEX idx_user_contract_user ON public.user_contract USING btree (user_id);

-- Permissions

ALTER TABLE public.user_contract OWNER TO postgres;
GRANT ALL ON TABLE public.user_contract TO postgres;


-- public.user_contract_department definition

-- Drop table

-- DROP TABLE public.user_contract_department;

CREATE TABLE public.user_contract_department ( user_contract_id uuid NOT NULL, department_id uuid NOT NULL, CONSTRAINT pk_ucd PRIMARY KEY (user_contract_id, department_id), CONSTRAINT fk_ucd_contract FOREIGN KEY (user_contract_id) REFERENCES public.user_contract(id) ON DELETE CASCADE, CONSTRAINT fk_ucd_dept FOREIGN KEY (department_id) REFERENCES public.department(id) ON DELETE CASCADE);
CREATE INDEX idx_ucd_dept ON public.user_contract_department USING btree (department_id);

-- Permissions

ALTER TABLE public.user_contract_department OWNER TO postgres;
GRANT ALL ON TABLE public.user_contract_department TO postgres;


-- public.admin_hospital_details definition

-- Drop table

-- DROP TABLE public.admin_hospital_details;

CREATE TABLE public.admin_hospital_details ( id uuid NOT NULL, office_location varchar(255) NULL, is_department_head bool DEFAULT false NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT admin_hospital_details_pkey PRIMARY KEY (id), CONSTRAINT admin_hospital_details_id_fkey FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.admin_hospital_details OWNER TO postgres;
GRANT ALL ON TABLE public.admin_hospital_details TO postgres;


-- public.appointment definition

-- Drop table

-- DROP TABLE public.appointment;

CREATE TABLE public.appointment ( id uuid NOT NULL, hospital_id uuid NOT NULL, patient_id uuid NOT NULL, doctor_id uuid NOT NULL, appointment_time timestamptz NOT NULL, token_number int4 NULL, session_period varchar(20) NULL, status varchar(30) NOT NULL, notes varchar(1000) NULL, chief_complaints text NULL, diagnosis text NULL, clinical_notes text NULL, icd_codes jsonb DEFAULT '[]'::jsonb NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, deleted bool DEFAULT false NOT NULL, patient_name varchar(255) NULL, doctor_name varchar(255) NULL, visit_id uuid NULL, appointment_date date NOT NULL, appointment_slot_time time NOT NULL, CONSTRAINT appointment_pkey PRIMARY KEY (id), CONSTRAINT chk_appointment_session_period CHECK (((session_period)::text = ANY (ARRAY[('MORNING'::character varying)::text, ('AFTERNOON'::character varying)::text, ('EVENING'::character varying)::text]))), CONSTRAINT chk_appointment_status CHECK (((status)::text = ANY (ARRAY[('SCHEDULED'::character varying)::text, ('PAYMENT_PENDING'::character varying)::text, ('PAID'::character varying)::text, ('VITALS_RECORDED'::character varying)::text, ('CONSULTATION_STARTED'::character varying)::text, ('PRESCRIPTION_FINALIZED'::character varying)::text, ('COMPLETED'::character varying)::text, ('CANCELLED'::character varying)::text]))), CONSTRAINT appointment_visit_id_fkey FOREIGN KEY (visit_id) REFERENCES public.patient_visit(id), CONSTRAINT fk_apt_dr FOREIGN KEY (doctor_id) REFERENCES public.doctor_profile(id), CONSTRAINT fk_apt_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_apt_pat FOREIGN KEY (patient_id) REFERENCES public.patient(id));
CREATE INDEX idx_appointment_visit ON public.appointment USING btree (visit_id);
CREATE INDEX idx_apt_time ON public.appointment USING btree (hospital_id, appointment_time);
CREATE UNIQUE INDEX uq_appointment_doctor_date_token ON public.appointment USING btree (doctor_id, appointment_time, token_number);

-- Permissions

ALTER TABLE public.appointment OWNER TO postgres;
GRANT ALL ON TABLE public.appointment TO postgres;


-- public.billing definition

-- Drop table

-- DROP TABLE public.billing;

CREATE TABLE public.billing ( id uuid NOT NULL, hospital_id uuid NOT NULL, patient_id uuid NOT NULL, appointment_id uuid NULL, source_type varchar(30) NOT NULL, status varchar(30) NOT NULL, total_amount numeric(12, 2) NOT NULL, discount numeric(12, 2) DEFAULT 0.00 NULL, payment_method varchar(50) NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, doctor_id uuid NULL, doctor_name varchar(255) NULL, CONSTRAINT billing_pkey PRIMARY KEY (id), CONSTRAINT billing_appointment_id_fkey FOREIGN KEY (appointment_id) REFERENCES public.appointment(id), CONSTRAINT fk_bill_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_bill_pat FOREIGN KEY (patient_id) REFERENCES public.patient(id));
CREATE INDEX idx_billing_appointment ON public.billing USING btree (appointment_id);

-- Permissions

ALTER TABLE public.billing OWNER TO postgres;
GRANT ALL ON TABLE public.billing TO postgres;


-- public.billing_item definition

-- Drop table

-- DROP TABLE public.billing_item;

CREATE TABLE public.billing_item ( id uuid NOT NULL, hospital_id uuid NOT NULL, billing_id uuid NOT NULL, item_name varchar(255) NOT NULL, amount numeric(10, 2) NOT NULL, quantity int4 NOT NULL, subtotal numeric(13, 2) NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, CONSTRAINT billing_item_pkey PRIMARY KEY (id), CONSTRAINT fk_billi_bill FOREIGN KEY (billing_id) REFERENCES public.billing(id), CONSTRAINT fk_billi_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id));
CREATE INDEX idx_billi_hospital ON public.billing_item USING btree (hospital_id);

-- Permissions

ALTER TABLE public.billing_item OWNER TO postgres;
GRANT ALL ON TABLE public.billing_item TO postgres;


-- public.doctor_availability_exception definition

-- Drop table

-- DROP TABLE public.doctor_availability_exception;

CREATE TABLE public.doctor_availability_exception ( id uuid NOT NULL, doctor_id uuid NOT NULL, hospital_id uuid NOT NULL, exception_date date NOT NULL, "type" varchar(20) NOT NULL, start_time time NULL, end_time time NULL, "mode" varchar(10) NULL, slot_duration_minutes int4 NULL, note text NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT chk_doctor_availability_exception_type CHECK (((type)::text = ANY (ARRAY[('FULL_DAY_OFF'::character varying)::text, ('MODIFIED'::character varying)::text, ('ADDED'::character varying)::text]))), CONSTRAINT doctor_availability_exception_pkey PRIMARY KEY (id), CONSTRAINT fk_ex_doctor FOREIGN KEY (doctor_id) REFERENCES public.doctor_profile(id) ON DELETE CASCADE, CONSTRAINT fk_ex_hospital FOREIGN KEY (hospital_id) REFERENCES public.hospital(id) ON DELETE CASCADE);
CREATE INDEX idx_dr_avail_ex_lookup ON public.doctor_availability_exception USING btree (doctor_id, hospital_id, exception_date);

-- Permissions

ALTER TABLE public.doctor_availability_exception OWNER TO postgres;
GRANT ALL ON TABLE public.doctor_availability_exception TO postgres;


-- public.doctor_hospital_details definition

-- Drop table

-- DROP TABLE public.doctor_hospital_details;

CREATE TABLE public.doctor_hospital_details ( id uuid NOT NULL, consultation_fee numeric(12, 2) DEFAULT 0.00 NULL, follow_up_days int4 DEFAULT 0 NULL, commission_type varchar(20) NULL, commission_value numeric(12, 2) NULL, is_visiting_consultant bool DEFAULT false NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT chk_doctor_hospital_details_commission_type CHECK (((commission_type)::text = ANY (ARRAY[('PERCENTAGE'::character varying)::text, ('FIXED'::character varying)::text]))), CONSTRAINT doctor_hospital_details_pkey PRIMARY KEY (id), CONSTRAINT doctor_hospital_details_id_fkey FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.doctor_hospital_details OWNER TO postgres;
GRANT ALL ON TABLE public.doctor_hospital_details TO postgres;


-- public.doctor_schedules definition

-- Drop table

-- DROP TABLE public.doctor_schedules;

CREATE TABLE public.doctor_schedules ( id uuid NOT NULL, assignment_id uuid NOT NULL, day_of_week varchar(20) NULL, start_time time NOT NULL, end_time time NOT NULL, slot_duration_minutes int4 DEFAULT 15 NULL, max_patients int4 NULL, CONSTRAINT chk_doctor_schedules_day_of_week CHECK (((day_of_week)::text = ANY (ARRAY[('MONDAY'::character varying)::text, ('TUESDAY'::character varying)::text, ('WEDNESDAY'::character varying)::text, ('THURSDAY'::character varying)::text, ('FRIDAY'::character varying)::text, ('SATURDAY'::character varying)::text, ('SUNDAY'::character varying)::text]))), CONSTRAINT doctor_schedules_pkey PRIMARY KEY (id), CONSTRAINT doctor_schedules_assignment_id_fkey FOREIGN KEY (assignment_id) REFERENCES public.user_contract(id) ON DELETE CASCADE);
CREATE INDEX idx_dr_schedule_assignment ON public.doctor_schedules USING btree (assignment_id, day_of_week);

-- Permissions

ALTER TABLE public.doctor_schedules OWNER TO postgres;
GRANT ALL ON TABLE public.doctor_schedules TO postgres;


-- public.inventory_stock definition

-- Drop table

-- DROP TABLE public.inventory_stock;

CREATE TABLE public.inventory_stock ( id uuid NOT NULL, hospital_id uuid NOT NULL, item_id uuid NOT NULL, quantity numeric(12, 2) NOT NULL, last_updated timestamp NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, "version" int8 DEFAULT 0 NOT NULL, CONSTRAINT inventory_stock_pkey PRIMARY KEY (id), CONSTRAINT uq_invs_hosp_itm UNIQUE (hospital_id, item_id), CONSTRAINT fk_invs_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_invs_itm FOREIGN KEY (item_id) REFERENCES public.item(id));
CREATE INDEX idx_invs_hosp ON public.inventory_stock USING btree (hospital_id);

-- Permissions

ALTER TABLE public.inventory_stock OWNER TO postgres;
GRANT ALL ON TABLE public.inventory_stock TO postgres;


-- public.inventory_transaction definition

-- Drop table

-- DROP TABLE public.inventory_transaction;

CREATE TABLE public.inventory_transaction ( id uuid NOT NULL, hospital_id uuid NOT NULL, item_id uuid NOT NULL, "type" varchar(50) NOT NULL, quantity numeric(12, 2) NOT NULL, reference varchar(255) NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, CONSTRAINT chk_inventory_transaction_type CHECK (((type)::text = ANY (ARRAY[('STOCK_IN'::character varying)::text, ('STOCK_OUT'::character varying)::text, ('ADJUSTMENT'::character varying)::text, ('RETURN'::character varying)::text]))), CONSTRAINT inventory_transaction_pkey PRIMARY KEY (id), CONSTRAINT fk_invt_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_invt_itm FOREIGN KEY (item_id) REFERENCES public.item(id));
CREATE INDEX idx_invt_hosp ON public.inventory_transaction USING btree (hospital_id);
CREATE INDEX idx_invt_item ON public.inventory_transaction USING btree (item_id);

-- Permissions

ALTER TABLE public.inventory_transaction OWNER TO postgres;
GRANT ALL ON TABLE public.inventory_transaction TO postgres;


-- public.invoice definition

-- Drop table

-- DROP TABLE public.invoice;

CREATE TABLE public.invoice ( id uuid NOT NULL, hospital_id uuid NOT NULL, patient_id uuid NULL, visit_id uuid NULL, invoice_number varchar(50) NOT NULL, status varchar(20) NOT NULL, subtotal_amount numeric(12, 2) DEFAULT 0 NOT NULL, tax_amount numeric(12, 2) DEFAULT 0 NOT NULL, discount_amount numeric(12, 2) DEFAULT 0 NOT NULL, total_amount numeric(12, 2) DEFAULT 0 NOT NULL, amount_paid numeric(12, 2) DEFAULT 0 NOT NULL, amount_due numeric(12, 2) GENERATED ALWAYS AS ((total_amount - amount_paid)) STORED NULL, due_date timestamp NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, customer_name varchar(255) NULL, CONSTRAINT chk_invoice_status CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('UNPAID'::character varying)::text, ('PARTIAL'::character varying)::text, ('PAID'::character varying)::text, ('CANCELLED'::character varying)::text]))), CONSTRAINT invoice_pkey PRIMARY KEY (id), CONSTRAINT uk_invoice_number UNIQUE (hospital_id, invoice_number), CONSTRAINT invoice_hospital_id_fkey FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT invoice_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient(id), CONSTRAINT invoice_visit_id_fkey FOREIGN KEY (visit_id) REFERENCES public.patient_visit(id));
CREATE INDEX idx_invoice_patient ON public.invoice USING btree (patient_id);
CREATE INDEX idx_invoice_status ON public.invoice USING btree (hospital_id, status);
CREATE INDEX idx_invoice_visit ON public.invoice USING btree (visit_id);
CREATE UNIQUE INDEX uq_invoice_visit ON public.invoice USING btree (visit_id) WHERE (visit_id IS NOT NULL);

-- Permissions

ALTER TABLE public.invoice OWNER TO postgres;
GRANT ALL ON TABLE public.invoice TO postgres;


-- public.invoice_item definition

-- Drop table

-- DROP TABLE public.invoice_item;

CREATE TABLE public.invoice_item ( id uuid NOT NULL, invoice_id uuid NOT NULL, hospital_id uuid NOT NULL, item_description varchar(255) NOT NULL, category varchar(50) NOT NULL, quantity numeric(10, 2) NOT NULL, unit_price numeric(12, 2) NOT NULL, tax_percentage numeric(5, 2) DEFAULT 0 NULL, tax_amount numeric(12, 2) DEFAULT 0 NULL, discount_amount numeric(12, 2) DEFAULT 0 NULL, subtotal numeric(12, 2) NOT NULL, net_total numeric(12, 2) NOT NULL, source_type varchar(50) NOT NULL, source_id uuid NOT NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, doctor_id uuid NULL, doctor_name varchar(255) NULL, hsn_code varchar(50) NULL, CONSTRAINT chk_invoice_item_category CHECK (((category)::text = ANY (ARRAY[('CONSULTATION'::character varying)::text, ('PHARMACY'::character varying)::text, ('LAB'::character varying)::text, ('PROCEDURE'::character varying)::text]))), CONSTRAINT chk_invoice_item_source_type CHECK (((source_type)::text = ANY (ARRAY[('APPOINTMENT'::character varying)::text, ('PHARMACY_SALE_ITEM'::character varying)::text, ('LAB_ORDER'::character varying)::text, ('CORRECTION'::character varying)::text, ('TEST'::character varying)::text]))), CONSTRAINT invoice_item_pkey PRIMARY KEY (id), CONSTRAINT invoice_item_hospital_id_fkey FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT invoice_item_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.invoice(id) ON DELETE CASCADE);
CREATE INDEX idx_inv_item_doctor ON public.invoice_item USING btree (doctor_id);
CREATE INDEX idx_inv_item_source ON public.invoice_item USING btree (source_type, source_id);

-- Permissions

ALTER TABLE public.invoice_item OWNER TO postgres;
GRANT ALL ON TABLE public.invoice_item TO postgres;


-- public.nurse_hospital_details definition

-- Drop table

-- DROP TABLE public.nurse_hospital_details;

CREATE TABLE public.nurse_hospital_details ( id uuid NOT NULL, floor_location varchar(255) NULL, shift_type varchar(50) NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT chk_nurse_hospital_details_shift_type CHECK (((shift_type)::text = ANY (ARRAY[('DAY'::character varying)::text, ('NIGHT'::character varying)::text, ('ROTATIONAL'::character varying)::text]))), CONSTRAINT nurse_hospital_details_pkey PRIMARY KEY (id), CONSTRAINT nurse_hospital_details_id_fkey FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.nurse_hospital_details OWNER TO postgres;
GRANT ALL ON TABLE public.nurse_hospital_details TO postgres;


-- public.payment_allocation definition

-- Drop table

-- DROP TABLE public.payment_allocation;

CREATE TABLE public.payment_allocation ( id uuid NOT NULL, payment_id uuid NOT NULL, invoice_id uuid NOT NULL, allocated_amount numeric(12, 2) NOT NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT payment_allocation_pkey PRIMARY KEY (id), CONSTRAINT payment_allocation_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.invoice(id), CONSTRAINT payment_allocation_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payment(id));

-- Table Triggers

create trigger trg_update_invoice_amount_paid after
insert
    or
delete
    on
    public.payment_allocation for each row execute function fn_update_invoice_amount_paid();

-- Permissions

ALTER TABLE public.payment_allocation OWNER TO postgres;
GRANT ALL ON TABLE public.payment_allocation TO postgres;


-- public.pharmacist_hospital_details definition

-- Drop table

-- DROP TABLE public.pharmacist_hospital_details;

CREATE TABLE public.pharmacist_hospital_details ( id uuid NOT NULL, counter_location varchar(255) NULL, pharmacy_wing varchar(100) NULL, pharmacy_license_no varchar(50) NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT pharmacist_hospital_details_pkey PRIMARY KEY (id), CONSTRAINT pharmacist_hospital_details_id_fkey FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.pharmacist_hospital_details OWNER TO postgres;
GRANT ALL ON TABLE public.pharmacist_hospital_details TO postgres;


-- public.pharmacy_sale_item definition

-- Drop table

-- DROP TABLE public.pharmacy_sale_item;

CREATE TABLE public.pharmacy_sale_item ( id uuid NOT NULL, sale_id uuid NOT NULL, medicine_id uuid NOT NULL, stock_id uuid NOT NULL, quantity int4 NOT NULL, unit_price numeric(12, 2) DEFAULT 0 NOT NULL, subtotal numeric(12, 2) DEFAULT 0 NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, discount_amount numeric(12, 2) DEFAULT 0 NULL, tax_amount numeric(12, 2) DEFAULT 0 NULL, net_amount numeric(12, 2) DEFAULT 0 NULL, CONSTRAINT pharmacy_sale_item_pkey PRIMARY KEY (id), CONSTRAINT fk_pharmacy_sale_item_medicine FOREIGN KEY (medicine_id) REFERENCES public.medicine(id), CONSTRAINT fk_pharmacy_sale_item_sale FOREIGN KEY (sale_id) REFERENCES public.pharmacy_sale(id) ON DELETE CASCADE, CONSTRAINT fk_pharmacy_sale_item_stock FOREIGN KEY (stock_id) REFERENCES public.pharmacy_stock(id));

-- Permissions

ALTER TABLE public.pharmacy_sale_item OWNER TO postgres;
GRANT ALL ON TABLE public.pharmacy_sale_item TO postgres;


-- public.prescription definition

-- Drop table

-- DROP TABLE public.prescription;

CREATE TABLE public.prescription ( id uuid NOT NULL, patient_id uuid NOT NULL, doctor_id uuid NOT NULL, hospital_id uuid NOT NULL, deleted bool DEFAULT false NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, appointment_id uuid NOT NULL, patient_name varchar(255) NULL, doctor_name varchar(255) NULL, status varchar(20) DEFAULT 'PENDING'::character varying NOT NULL, CONSTRAINT chk_prescription_status CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('DISPENSED'::character varying)::text, ('CANCELLED'::character varying)::text]))), CONSTRAINT prescription_pkey PRIMARY KEY (id), CONSTRAINT fk_pre_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_prescription_appointment FOREIGN KEY (appointment_id) REFERENCES public.appointment(id) ON DELETE CASCADE, CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES public.doctor_profile(id), CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES public.patient(id));
CREATE INDEX idx_prescription_appointment ON public.prescription USING btree (appointment_id);
CREATE INDEX idx_prescription_doctor ON public.prescription USING btree (doctor_id);
CREATE INDEX idx_prescription_hospital ON public.prescription USING btree (hospital_id);
CREATE INDEX idx_prescription_patient ON public.prescription USING btree (patient_id);

-- Permissions

ALTER TABLE public.prescription OWNER TO postgres;
GRANT ALL ON TABLE public.prescription TO postgres;


-- public.prescription_item definition

-- Drop table

-- DROP TABLE public.prescription_item;

CREATE TABLE public.prescription_item ( id uuid NOT NULL, prescription_id uuid NOT NULL, medicine_id uuid NULL, dosage varchar(255) NOT NULL, duration varchar(255) NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, created_by varchar(255) NULL, updated_by varchar(255) NULL, hospital_id uuid NOT NULL, medicine_name varchar(255) NULL, CONSTRAINT prescription_item_pkey PRIMARY KEY (id), CONSTRAINT fk_prei_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_prei_med FOREIGN KEY (medicine_id) REFERENCES public.medicine(id), CONSTRAINT fk_prei_pre FOREIGN KEY (prescription_id) REFERENCES public.prescription(id) ON DELETE CASCADE);
CREATE INDEX idx_prescription_item_hospital ON public.prescription_item USING btree (hospital_id);
CREATE INDEX idx_prescription_item_prescription ON public.prescription_item USING btree (prescription_id);

-- Permissions

ALTER TABLE public.prescription_item OWNER TO postgres;
GRANT ALL ON TABLE public.prescription_item TO postgres;


-- public.receptionist_hospital_details definition

-- Drop table

-- DROP TABLE public.receptionist_hospital_details;

CREATE TABLE public.receptionist_hospital_details ( id uuid NOT NULL, desk_location varchar(255) NULL, emergency_contact_number varchar(20) NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT receptionist_hospital_details_pkey PRIMARY KEY (id), CONSTRAINT receptionist_hospital_details_id_fkey FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE);

-- Permissions

ALTER TABLE public.receptionist_hospital_details OWNER TO postgres;
GRANT ALL ON TABLE public.receptionist_hospital_details TO postgres;


-- public.refund definition

-- Drop table

-- DROP TABLE public.refund;

CREATE TABLE public.refund ( id uuid NOT NULL, hospital_id uuid NOT NULL, payment_id uuid NOT NULL, invoice_id uuid NOT NULL, refund_amount numeric(12, 2) NOT NULL, reason varchar(500) NOT NULL, status varchar(20) NOT NULL, source_type varchar(50) NULL, source_id uuid NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, processed_at timestamptz NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, CONSTRAINT chk_refund_source_type CHECK (((source_type)::text = ANY (ARRAY[('APPOINTMENT'::character varying)::text, ('PHARMACY_SALE_ITEM'::character varying)::text, ('LAB_ORDER'::character varying)::text, ('CORRECTION'::character varying)::text, ('TEST'::character varying)::text]))), CONSTRAINT chk_refund_status CHECK (((status)::text = ANY (ARRAY[('INITIATED'::character varying)::text, ('COMPLETED'::character varying)::text]))), CONSTRAINT refund_pkey PRIMARY KEY (id), CONSTRAINT refund_hospital_id_fkey FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT refund_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.invoice(id), CONSTRAINT refund_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payment(id));
CREATE INDEX idx_refund_payment ON public.refund USING btree (payment_id);

-- Permissions

ALTER TABLE public.refund OWNER TO postgres;
GRANT ALL ON TABLE public.refund TO postgres;


-- public.vitals definition

-- Drop table

-- DROP TABLE public.vitals;

CREATE TABLE public.vitals ( id uuid NOT NULL, systolic int4 NULL, diastolic int4 NULL, heart_rate int4 NULL, temperature numeric(5, 2) NULL, weight numeric(5, 2) NULL, oxygen_level numeric(5, 2) NULL, blood_pressure varchar(20) NULL, hospital_id uuid NOT NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, CONSTRAINT vitals_pkey PRIMARY KEY (id), CONSTRAINT fk_vit_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_vitals_appointment FOREIGN KEY (id) REFERENCES public.appointment(id) ON DELETE CASCADE);
CREATE INDEX idx_vitals_hospital ON public.vitals USING btree (hospital_id);

-- Permissions

ALTER TABLE public.vitals OWNER TO postgres;
GRANT ALL ON TABLE public.vitals TO postgres;


-- public.pharmacy_return definition

-- Drop table

-- DROP TABLE public.pharmacy_return;

CREATE TABLE public.pharmacy_return ( id uuid NOT NULL, hospital_id uuid NOT NULL, sale_item_id uuid NOT NULL, stock_id uuid NOT NULL, quantity int4 NOT NULL, reason varchar(500) NULL, created_at timestamp NOT NULL, updated_at timestamp NOT NULL, CONSTRAINT pharmacy_return_pkey PRIMARY KEY (id), CONSTRAINT fk_pharmacy_return_hospital FOREIGN KEY (hospital_id) REFERENCES public.hospital(id), CONSTRAINT fk_pharmacy_return_sale_item FOREIGN KEY (sale_item_id) REFERENCES public.pharmacy_sale_item(id), CONSTRAINT fk_pharmacy_return_stock FOREIGN KEY (stock_id) REFERENCES public.pharmacy_stock(id));

-- Permissions

ALTER TABLE public.pharmacy_return OWNER TO postgres;
GRANT ALL ON TABLE public.pharmacy_return TO postgres;
