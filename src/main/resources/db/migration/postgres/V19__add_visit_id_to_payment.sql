-- V19__add_visit_id_to_payment.sql
ALTER TABLE public.payment ADD COLUMN visit_id uuid REFERENCES public.patient_visit(id);
CREATE INDEX idx_payment_visit ON public.payment(visit_id);
