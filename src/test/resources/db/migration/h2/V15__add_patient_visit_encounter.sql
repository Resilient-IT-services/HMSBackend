-- V15__add_patient_visit_encounter.sql
-- 1. Create Patient Visit (Encounter) Table
CREATE TABLE public.patient_visit (
    id uuid NOT NULL PRIMARY KEY,
    hospital_id uuid NOT NULL REFERENCES public.hospital(id),
    patient_id uuid NOT NULL REFERENCES public.patient(id),
    visit_number varchar(50) NOT NULL,
    visit_type varchar(20) NOT NULL, -- 'OPD', 'IPD', 'EMERGENCY', 'WALKIN'
    status varchar(20) NOT NULL, -- 'ACTIVE', 'DISCHARGED', 'CANCELLED'
    started_at timestamp NOT NULL,
    ended_at timestamp NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_visit_number UNIQUE (hospital_id, visit_number)
);

CREATE INDEX idx_visit_patient ON public.patient_visit(patient_id);
CREATE INDEX idx_visit_hospital_status ON public.patient_visit(hospital_id, status);

-- 2. Link existing operational tables to Visit (Nullable for backward compatibility)
ALTER TABLE public.appointment ADD COLUMN visit_id uuid REFERENCES public.patient_visit(id);
ALTER TABLE public.pharmacy_sale ADD COLUMN visit_id uuid REFERENCES public.patient_visit(id);

CREATE INDEX idx_appointment_visit ON public.appointment(visit_id);
CREATE INDEX idx_pharmacy_sale_visit ON public.pharmacy_sale(visit_id);
