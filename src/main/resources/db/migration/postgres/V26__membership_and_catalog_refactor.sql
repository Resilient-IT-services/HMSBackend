-- V26__membership_and_catalog_refactor.sql

-- 1. Reference Data Tables
CREATE TABLE IF NOT EXISTS public.department (
    id uuid PRIMARY KEY,
    name varchar(255) UNIQUE NOT NULL,
    description text,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.specialization (
    id uuid PRIMARY KEY,
    name varchar(255) UNIQUE NOT NULL,
    description text,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Insert Default Departments and Specializations
-- General Department
INSERT INTO public.department (id, name, description, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'General', 'General Department', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Cardiology Department
INSERT INTO public.department (id, name, description, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'Cardiology', 'Cardiology Department', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- General Medicine Specialization
INSERT INTO public.specialization (id, name, description, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'General Medicine', 'General Medicine', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Cardiology Specialization
INSERT INTO public.specialization (id, name, description, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'Cardiology', 'Cardiology', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 3. Rename user_role_assignment to user_contract if it exists and user_contract doesn't
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'user_role_assignment')
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'user_contract') THEN
        
        -- Rename the table
        ALTER TABLE public.user_role_assignment RENAME TO user_contract;
        
        -- Rename the primary key constraint
        ALTER INDEX IF EXISTS user_role_assignment_pkey RENAME TO user_contract_pkey;
        
        -- Rename foreign keys from user_contract
        ALTER TABLE public.user_contract RENAME CONSTRAINT fk_assign_hosp TO fk_contract_hosp;
        ALTER TABLE public.user_contract RENAME CONSTRAINT fk_assign_role TO fk_contract_role;
        ALTER TABLE public.user_contract RENAME CONSTRAINT fk_assign_user TO fk_contract_user;
    END IF;
END $$;

-- 4. Create Join Tables
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

-- 5. Data Migration (Ensure no data loss)

-- Link all hospitals to 'General' department
INSERT INTO public.hospital_departments (hospital_id, department_id)
SELECT h.id, d.id
FROM public.hospital h
CROSS JOIN public.department d
WHERE d.name = 'General'
ON CONFLICT DO NOTHING;

-- Link all user_contracts to 'General' department
INSERT INTO public.user_contract_department (user_contract_id, department_id)
SELECT uc.id, d.id
FROM public.user_contract uc
CROSS JOIN public.department d
WHERE d.name = 'General'
ON CONFLICT DO NOTHING;

-- Link all doctors to 'General Medicine' specialization
INSERT INTO public.doctor_specialization (doctor_profile_id, specialization_id)
SELECT dp.id, s.id
FROM public.doctor_profile dp
CROSS JOIN public.specialization s
WHERE s.name = 'General Medicine'
ON CONFLICT DO NOTHING;

-- 6. Cleanup deprecated columns
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_contract' AND column_name = 'department_name') THEN
        ALTER TABLE public.user_contract DROP COLUMN department_name;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'doctor_profile' AND column_name = 'specialization') THEN
        ALTER TABLE public.doctor_profile DROP COLUMN specialization;
    END IF;
END $$;

-- 7. Add new columns to profile tables
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

-- 8. Add missing indexes
CREATE INDEX IF NOT EXISTS idx_user_contract_user ON public.user_contract (user_id);
CREATE INDEX IF NOT EXISTS idx_user_contract_hospital ON public.user_contract (hospital_id);
CREATE INDEX IF NOT EXISTS idx_ucd_dept ON public.user_contract_department (department_id);
CREATE INDEX IF NOT EXISTS idx_ds_spec ON public.doctor_specialization (specialization_id);
