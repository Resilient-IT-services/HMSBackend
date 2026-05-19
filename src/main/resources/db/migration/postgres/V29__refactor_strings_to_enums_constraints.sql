-- V29__refactor_strings_to_enums_constraints.sql

-- 1. patient.blood_group
ALTER TABLE public.patient ADD CONSTRAINT chk_patient_blood_group 
CHECK (blood_group IN ('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'));

-- 2. prescription.status
ALTER TABLE public.prescription ADD CONSTRAINT chk_prescription_status 
CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'));

-- 3. doctor_availability_exception.type
ALTER TABLE public.doctor_availability_exception ADD CONSTRAINT chk_doctor_availability_exception_type 
CHECK (type IN ('FULL_DAY_OFF', 'MODIFIED', 'ADDED'));

-- 4. doctor_schedules.day_of_week (Converting from Short to String in DB if mapped to Enum)
-- Wait, the Java field is now DayOfWeek (Enum), and I used @Enumerated(STRING).
-- The DB column was Short (1-7). I need to convert the column to VARCHAR.
ALTER TABLE public.doctor_schedules ALTER COLUMN day_of_week TYPE varchar(20) USING 
  CASE day_of_week 
    WHEN 1 THEN 'MONDAY' 
    WHEN 2 THEN 'TUESDAY' 
    WHEN 3 THEN 'WEDNESDAY' 
    WHEN 4 THEN 'THURSDAY' 
    WHEN 5 THEN 'FRIDAY' 
    WHEN 6 THEN 'SATURDAY' 
    WHEN 7 THEN 'SUNDAY' 
  END;
ALTER TABLE public.doctor_schedules ADD CONSTRAINT chk_doctor_schedules_day_of_week 
CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));

-- 5. medicine.form and medicine.schedule_type
ALTER TABLE public.medicine ADD CONSTRAINT chk_medicine_form 
CHECK (form IN ('TABLET', 'SYRUP', 'INJECTION', 'CAPSULE', 'OINTMENT', 'CREAM', 'DROPS', 'INHALER'));

ALTER TABLE public.medicine ADD CONSTRAINT chk_medicine_schedule_type 
CHECK (schedule_type IN ('SCHEDULE_H', 'SCHEDULE_G', 'SCHEDULE_X', 'OTC'));

-- 6. pharmacy_sale.sale_type and pharmacy_sale.status
ALTER TABLE public.pharmacy_sale ADD CONSTRAINT chk_pharmacy_sale_sale_type 
CHECK (sale_type IN ('VISIT', 'WALKIN'));

ALTER TABLE public.pharmacy_sale ADD CONSTRAINT chk_pharmacy_sale_status 
CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED'));

-- 7. item.unit
ALTER TABLE public.item ADD CONSTRAINT chk_item_unit 
CHECK (unit IN ('PIECE', 'BOX', 'STRIP', 'BOTTLE', 'PACK', 'VIAL', 'AMPULE'));

-- 8. invoice_item.source_type
ALTER TABLE public.invoice_item ADD CONSTRAINT chk_invoice_item_source_type 
CHECK (source_type IN ('APPOINTMENT', 'PHARMACY_SALE_ITEM', 'LAB_ORDER'));

-- 9. refund.source_type
ALTER TABLE public.refund ADD CONSTRAINT chk_refund_source_type 
CHECK (source_type IN ('APPOINTMENT', 'PHARMACY_SALE_ITEM', 'LAB_ORDER'));

-- 10. audit_log.entity_name
ALTER TABLE public.audit_log ADD CONSTRAINT chk_audit_log_entity_name 
CHECK (entity_name IN ('PATIENT', 'APPOINTMENT', 'INVOICE', 'PAYMENT', 'PRESCRIPTION', 'MEDICINE', 'HOSPITAL', 'USER_ACCOUNT', 'USER_CONTRACT'));
