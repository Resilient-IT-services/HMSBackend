-- V26__membership_and_catalog_refactor.sql

-- 1. Reference Data Tables
CREATE TABLE IF NOT EXISTS public.department (
    id uuid PRIMARY KEY,
    name varchar(255) UNIQUE NOT NULL,
    description text,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS public.specialization (
    id uuid PRIMARY KEY,
    name varchar(255) UNIQUE NOT NULL,
    description text,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

-- 2. Rename user_role_assignment to user_contract if it exists and user_contract doesn't
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'user_role_assignment') 
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'user_contract') THEN
        ALTER TABLE public.user_role_assignment RENAME TO user_contract;
    END IF;
END $$;

-- 3. Cleanup user_contract (remove old department_name)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_contract' AND column_name = 'department_name') THEN
        ALTER TABLE public.user_contract DROP COLUMN department_name;
    END IF;
END $$;

-- 4. Join Tables
CREATE TABLE IF NOT EXISTS public.user_contract_department (
    user_contract_id uuid NOT NULL,
    department_id uuid NOT NULL,
    CONSTRAINT pk_ucd PRIMARY KEY (user_contract_id, department_id),
    CONSTRAINT fk_ucd_contract FOREIGN KEY (user_contract_id) REFERENCES public.user_contract(id) ON DELETE CASCADE,
    CONSTRAINT fk_ucd_dept FOREIGN KEY (department_id) REFERENCES public.department(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.hospital_departments (
    hospital_id uuid NOT NULL,
    department_id uuid NOT NULL,
    CONSTRAINT pk_hd PRIMARY KEY (hospital_id, department_id),
    CONSTRAINT fk_hd_hospital FOREIGN KEY (hospital_id) REFERENCES public.hospital(id) ON DELETE CASCADE,
    CONSTRAINT fk_hd_dept FOREIGN KEY (department_id) REFERENCES public.department(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.doctor_specialization (
    doctor_profile_id uuid NOT NULL,
    specialization_id uuid NOT NULL,
    CONSTRAINT pk_ds PRIMARY KEY (doctor_profile_id, specialization_id),
    CONSTRAINT fk_ds_doctor FOREIGN KEY (doctor_profile_id) REFERENCES public.doctor_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_ds_spec FOREIGN KEY (specialization_id) REFERENCES public.specialization(id) ON DELETE CASCADE
);

-- 5. Refactor profile tables for consistency with Java entities
-- doctor_profile refactor
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'doctor_profile' AND column_name = 'specialization') THEN
        ALTER TABLE public.doctor_profile DROP COLUMN specialization;
    END IF;
END $$;

ALTER TABLE public.doctor_profile ADD COLUMN IF NOT EXISTS registration_number varchar(100);
ALTER TABLE public.doctor_profile ADD COLUMN IF NOT EXISTS experience_years int4;
ALTER TABLE public.doctor_profile ADD COLUMN IF NOT EXISTS awards_json jsonb DEFAULT '[]';
ALTER TABLE public.doctor_profile ADD COLUMN IF NOT EXISTS languages_json jsonb DEFAULT '[]';
ALTER TABLE public.doctor_profile ADD COLUMN IF NOT EXISTS signature_url varchar(255);
ALTER TABLE public.doctor_profile ADD COLUMN IF NOT EXISTS consultation_fee numeric(19, 2);
ALTER TABLE public.doctor_profile ADD COLUMN IF NOT EXISTS metadata_json jsonb DEFAULT '{}';

-- receptionist_profile refactor
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'receptionist_profile' AND column_name = 'languages_spoken') THEN
        ALTER TABLE public.receptionist_profile RENAME COLUMN languages_spoken TO languages_spoken_json;
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'receptionist_profile' AND column_name = 'skills_json') THEN
        ALTER TABLE public.receptionist_profile DROP COLUMN skills_json;
    END IF;
END $$;

-- 6. Membership-Specific Detail Tables
CREATE TABLE IF NOT EXISTS public.doctor_hospital_details (
    id uuid PRIMARY KEY,
    consultation_fee numeric(19, 2) NOT NULL DEFAULT 0,
    follow_up_days int4 NOT NULL DEFAULT 0,
    commission_type varchar(50),
    commission_value numeric(19, 2),
    is_visiting_consultant bool DEFAULT false NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_dhd_contract FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.nurse_hospital_details (
    id uuid PRIMARY KEY,
    floor_location varchar(255),
    shift_type varchar(50),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_nhd_contract FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.admin_hospital_details (
    id uuid PRIMARY KEY,
    office_location varchar(255),
    is_department_head bool DEFAULT false NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_ahd_contract FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.pharmacist_hospital_details (
    id uuid PRIMARY KEY,
    counter_location varchar(255),
    pharmacy_wing varchar(255),
    pharmacy_license_no varchar(255),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_phd_contract FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.receptionist_hospital_details (
    id uuid PRIMARY KEY,
    desk_location varchar(255),
    emergency_contact_number varchar(255),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_rhd_contract FOREIGN KEY (id) REFERENCES public.user_contract(id) ON DELETE CASCADE
);

-- 7. Add missing indexes
CREATE INDEX IF NOT EXISTS idx_user_contract_user ON public.user_contract (user_id);
CREATE INDEX IF NOT EXISTS idx_user_contract_hospital ON public.user_contract (hospital_id);
CREATE INDEX IF NOT EXISTS idx_ucd_dept ON public.user_contract_department (department_id);
CREATE INDEX IF NOT EXISTS idx_ds_spec ON public.doctor_specialization (specialization_id);
