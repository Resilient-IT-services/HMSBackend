-- V27__update_appointment_statuses.sql
-- Move all appointments with invalid/deprecated statuses to 'CANCELLED'
UPDATE public.appointment 
SET status = 'CANCELLED' 
WHERE status NOT IN ('SCHEDULED', 'PAYMENT_PENDING', 'PAID', 'VITALS_RECORDED', 'CONSULTATION_STARTED', 'PRESCRIPTION_FINALIZED', 'COMPLETED', 'CANCELLED');
