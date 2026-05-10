-- V13__fix_prescription_foreign_key.sql
-- Fix the foreign key constraint on prescription table.
-- It was incorrectly placed on the 'id' column instead of 'appointment_id'.

-- 1. Drop the incorrect constraint on 'id'
ALTER TABLE public.prescription DROP CONSTRAINT IF EXISTS fk_prescription_appointment;

-- 2. Ensure appointment_id is NOT NULL
-- We first need to make sure no nulls exist, but since it was just added in V12 and backfilled from id, it should be fine.
ALTER TABLE public.prescription ALTER COLUMN appointment_id SET NOT NULL;

-- 3. Add the correct constraint on 'appointment_id'
ALTER TABLE public.prescription 
ADD CONSTRAINT fk_prescription_appointment 
FOREIGN KEY (appointment_id) REFERENCES public.appointment(id) ON DELETE CASCADE;

-- 4. Add index for performance if not already present
CREATE INDEX IF NOT EXISTS idx_prescription_appointment ON public.prescription (appointment_id);
