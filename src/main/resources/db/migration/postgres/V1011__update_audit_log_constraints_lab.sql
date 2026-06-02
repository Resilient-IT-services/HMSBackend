-- V1011__update_audit_log_constraints_lab.sql
-- Update the check constraint on audit_log to include LABORATORY_TEST

ALTER TABLE platform.audit_log DROP CONSTRAINT IF EXISTS chk_audit_log_entity_name;

ALTER TABLE platform.audit_log ADD CONSTRAINT chk_audit_log_entity_name 
CHECK (entity_name IN (
    'PATIENT', 
    'APPOINTMENT', 
    'INVOICE', 
    'PAYMENT', 
    'PRESCRIPTION', 
    'MEDICINE', 
    'HOSPITAL', 
    'USER_ACCOUNT', 
    'USER_CONTRACT', 
    'LABORATORY_TEST'
));
