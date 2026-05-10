-- V12__ddd_refactors.sql

-- 1. Denormalize Appointment (V10 handled this, but ensuring just in case)
ALTER TABLE public.appointment ADD COLUMN IF NOT EXISTS patient_name varchar(255);
ALTER TABLE public.appointment ADD COLUMN IF NOT EXISTS doctor_name varchar(255);

-- 2. Refactor Prescription for DDD isolation
ALTER TABLE public.prescription ADD COLUMN IF NOT EXISTS appointment_id uuid;
ALTER TABLE public.prescription ADD COLUMN IF NOT EXISTS patient_name varchar(255);
ALTER TABLE public.prescription ADD COLUMN IF NOT EXISTS doctor_name varchar(255);

-- 3. Refactor PrescriptionItem for DDD isolation
ALTER TABLE public.prescription_item ADD COLUMN IF NOT EXISTS medicine_name varchar(255);

-- 4. Backfill Appointment data
UPDATE public.appointment a SET 
    patient_name = p.full_name,
    doctor_name = u.full_name
FROM public.patient p, public.user_account u, public.doctor_profile d
WHERE a.patient_id = p.id AND a.doctor_id = d.id AND d.id = u.id AND (a.patient_name IS NULL OR a.doctor_name IS NULL);

-- 5. Backfill Prescription data
UPDATE public.prescription p SET 
    appointment_id = p.id, -- Old 1:1 align
    patient_name = a.patient_name,
    doctor_name = a.doctor_name
FROM public.appointment a
WHERE p.id = a.id AND (p.appointment_id IS NULL OR p.patient_name IS NULL);

-- 6. Backfill PrescriptionItem data
UPDATE public.prescription_item pi SET 
    medicine_name = m.name
FROM public.medicine m
WHERE pi.medicine_id = m.id AND pi.medicine_name IS NULL;

-- 7. Fix Billing denormalization from previous Phase 2
ALTER TABLE public.billing ADD COLUMN IF NOT EXISTS doctor_id uuid;
ALTER TABLE public.billing ADD COLUMN IF NOT EXISTS doctor_name varchar(255);

UPDATE public.billing b SET 
    doctor_id = a.doctor_id,
    doctor_name = a.doctor_name
FROM public.appointment a
WHERE b.appointment_id = a.id AND (b.doctor_id IS NULL OR b.doctor_name IS NULL);
