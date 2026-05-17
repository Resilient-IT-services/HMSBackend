-- V25__add_idempotency_constraints.sql

-- 1. Cleanup: Remove duplicate pharmacy sales for the same prescription
-- This keeps only the oldest record (earliest created_at) to resolve the UNIQUE constraint conflict
DELETE FROM public.pharmacy_sale a
USING public.pharmacy_sale b
WHERE a.id > b.id 
  AND a.prescription_id = b.prescription_id 
  AND a.prescription_id IS NOT NULL;

-- 2. Cleanup: Remove duplicate invoices for the same patient visit
DELETE FROM public.invoice a
USING public.invoice b
WHERE a.id > b.id 
  AND a.visit_id = b.visit_id 
  AND a.visit_id IS NOT NULL;

-- 3. Ensure a prescription can only be dispensed once
DROP INDEX IF EXISTS uq_pharmacy_sale_prescription;
CREATE UNIQUE INDEX uq_pharmacy_sale_prescription ON public.pharmacy_sale (prescription_id) WHERE prescription_id IS NOT NULL;

-- 4. Ensure a patient visit can only have one associated invoice
-- This prevents duplicate billing for the same encounter
DROP INDEX IF EXISTS uq_invoice_visit;
CREATE UNIQUE INDEX uq_invoice_visit ON public.invoice (visit_id) WHERE visit_id IS NOT NULL;
