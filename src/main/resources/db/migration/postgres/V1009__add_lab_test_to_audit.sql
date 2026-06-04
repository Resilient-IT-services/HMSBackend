ALTER TABLE platform.audit_log DROP CONSTRAINT chk_audit_log_entity_name;
ALTER TABLE platform.audit_log ADD CONSTRAINT chk_audit_log_entity_name CHECK (entity_name IN ('PATIENT', 'APPOINTMENT', 'INVOICE', 'PAYMENT', 'PRESCRIPTION', 'MEDICINE', 'HOSPITAL', 'USER_ACCOUNT', 'USER_CONTRACT', 'LAB_TEST'));
