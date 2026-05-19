-- V28__add_enum_check_constraints.sql

-- 1. audit_log.action
ALTER TABLE public.audit_log ADD CONSTRAINT chk_audit_log_action 
CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'UPDATE_STATUS'));

-- 2. appointment.status and appointment.session_period
ALTER TABLE public.appointment ADD CONSTRAINT chk_appointment_status 
CHECK (status IN ('SCHEDULED', 'PAYMENT_PENDING', 'PAID', 'VITALS_RECORDED', 'CONSULTATION_STARTED', 'PRESCRIPTION_FINALIZED', 'COMPLETED', 'CANCELLED'));

ALTER TABLE public.appointment ADD CONSTRAINT chk_appointment_session_period 
CHECK (session_period IN ('MORNING', 'AFTERNOON', 'EVENING'));

-- 3. patient.gender
ALTER TABLE public.patient ADD CONSTRAINT chk_patient_gender 
CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- 4. patient_visit.visit_type and patient_visit.status
ALTER TABLE public.patient_visit ADD CONSTRAINT chk_patient_visit_type 
CHECK (visit_type IN ('OPD', 'IPD', 'EMERGENCY', 'WALKIN'));

ALTER TABLE public.patient_visit ADD CONSTRAINT chk_patient_visit_status 
CHECK (status IN ('ACTIVE', 'DISCHARGED', 'CANCELLED'));

-- 5. invoice.status
ALTER TABLE public.invoice ADD CONSTRAINT chk_invoice_status 
CHECK (status IN ('DRAFT', 'UNPAID', 'PARTIAL', 'PAID', 'CANCELLED'));

-- 6. invoice_item.category
ALTER TABLE public.invoice_item ADD CONSTRAINT chk_invoice_item_category 
CHECK (category IN ('CONSULTATION', 'PHARMACY', 'LAB', 'PROCEDURE'));

-- 7. payment.payment_mode and payment.status
ALTER TABLE public.payment ADD CONSTRAINT chk_payment_mode 
CHECK (payment_mode IN ('CASH', 'CARD', 'UPI', 'NEFT', 'INSURANCE'));

ALTER TABLE public.payment ADD CONSTRAINT chk_payment_status 
CHECK (status IN ('SUCCESS', 'FAILED', 'REFUNDED'));

-- 8. refund.status
ALTER TABLE public.refund ADD CONSTRAINT chk_refund_status 
CHECK (status IN ('INITIATED', 'COMPLETED'));

-- 9. hospital.status
ALTER TABLE public.hospital ADD CONSTRAINT chk_hospital_status 
CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'ONBOARDING'));

-- 10. vital_range_configs.vital_type
ALTER TABLE public.vital_range_configs ADD CONSTRAINT chk_vital_range_configs_vital_type 
CHECK (vital_type IN ('BODY_TEMPERATURE', 'SYSTOLIC_BP', 'DIASTOLIC_BP', 'OXYGEN_LEVEL', 'WEIGHT', 'HEART_RATE'));

-- 11. doctor_hospital_details.commission_type
ALTER TABLE public.doctor_hospital_details ADD CONSTRAINT chk_doctor_hospital_details_commission_type 
CHECK (commission_type IN ('PERCENTAGE', 'FIXED'));

-- 12. nurse_hospital_details.shift_type
ALTER TABLE public.nurse_hospital_details ADD CONSTRAINT chk_nurse_hospital_details_shift_type 
CHECK (shift_type IN ('DAY', 'NIGHT', 'ROTATIONAL'));

-- 13. nurse_profile.certification_level
ALTER TABLE public.nurse_profile ADD CONSTRAINT chk_nurse_profile_certification_level 
CHECK (certification_level IN ('RN', 'LPN', 'NP', 'CNA'));

-- 14. role.code
ALTER TABLE public."role" ADD CONSTRAINT chk_role_code 
CHECK (code IN ('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'PHARMACIST'));

-- 15. user_account.gender
ALTER TABLE public.user_account ADD CONSTRAINT chk_user_account_gender 
CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- 16. inventory_transaction.type
ALTER TABLE public.inventory_transaction ADD CONSTRAINT chk_inventory_transaction_type 
CHECK (type IN ('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT', 'RETURN'));
