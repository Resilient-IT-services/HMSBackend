-- Drop status column and its constraint from prescription table
ALTER TABLE public.prescription DROP CONSTRAINT IF EXISTS chk_prescription_status;
ALTER TABLE public.prescription DROP COLUMN IF EXISTS status;
