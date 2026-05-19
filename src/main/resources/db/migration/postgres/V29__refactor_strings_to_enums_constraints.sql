-- V29__refactor_strings_to_enums_constraints.sql

-- 1. patient.blood_group
UPDATE public.patient
SET blood_group =
  CASE lower(trim(blood_group::text))
    WHEN 'a_positive' THEN 'A_POSITIVE'
    WHEN 'a_negative' THEN 'A_NEGATIVE'
    WHEN 'b_positive' THEN 'B_POSITIVE'
    WHEN 'b_negative' THEN 'B_NEGATIVE'
    WHEN 'ab_positive' THEN 'AB_POSITIVE'
    WHEN 'ab_negative' THEN 'AB_NEGATIVE'
    WHEN 'o_positive' THEN 'O_POSITIVE'
    WHEN 'o_negative' THEN 'O_NEGATIVE'
    ELSE 'O_NEGATIVE'
  END;

ALTER TABLE public.patient ADD CONSTRAINT chk_patient_blood_group
CHECK (blood_group IN ('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'));

-- 2. prescription.status
UPDATE public.prescription
SET status =
  CASE lower(trim(status::text))
    WHEN 'active' THEN 'ACTIVE'
    WHEN 'completed' THEN 'COMPLETED'
    WHEN 'cancelled' THEN 'CANCELLED'
    WHEN 'dispensed' THEN 'DISPENSED'
    ELSE 'DISPENSED'
  END;

ALTER TABLE public.prescription ADD CONSTRAINT chk_prescription_status
CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED','DISPENSED'));

-- 3. doctor_availability_exception.type
UPDATE public.doctor_availability_exception
SET type =
  CASE lower(trim(type::text))
    WHEN 'full_day_off' THEN 'FULL_DAY_OFF'
    WHEN 'modified' THEN 'MODIFIED'
    WHEN 'added' THEN 'ADDED'
    ELSE 'ADDED'
  END;

ALTER TABLE public.doctor_availability_exception ADD CONSTRAINT chk_doctor_availability_exception_type
CHECK (type IN ('FULL_DAY_OFF', 'MODIFIED', 'ADDED'));

-- 4. doctor_schedules.day_of_week (Converting from Short to String in DB if mapped to Enum)
-- Wait, the Java field is now DayOfWeek (Enum), and I used @Enumerated(STRING).
-- The DB column was Short (1-7). I need to convert the column to VARCHAR.
ALTER TABLE public.doctor_schedules
  DROP CONSTRAINT IF EXISTS doctor_schedules_day_of_week_check;

ALTER TABLE public.doctor_schedules ALTER COLUMN day_of_week TYPE varchar(20) USING
  CASE
    WHEN trim(day_of_week::text) IN ('1', 'MONDAY') THEN 'MONDAY'
    WHEN trim(day_of_week::text) IN ('2', 'TUESDAY') THEN 'TUESDAY'
    WHEN trim(day_of_week::text) IN ('3', 'WEDNESDAY') THEN 'WEDNESDAY'
    WHEN trim(day_of_week::text) IN ('4', 'THURSDAY') THEN 'THURSDAY'
    WHEN trim(day_of_week::text) IN ('5', 'FRIDAY') THEN 'FRIDAY'
    WHEN trim(day_of_week::text) IN ('6', 'SATURDAY') THEN 'SATURDAY'
    WHEN trim(day_of_week::text) IN ('7', 'SUNDAY') THEN 'SUNDAY'
    ELSE NULL
  END;
ALTER TABLE public.doctor_schedules ADD CONSTRAINT chk_doctor_schedules_day_of_week
CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));

-- 5. medicine.form and medicine.schedule_type
UPDATE public.medicine
SET form =
  CASE lower(trim(form::text))
    WHEN 'tablet' THEN 'TABLET'
    WHEN 'syrup' THEN 'SYRUP'
    WHEN 'injection' THEN 'INJECTION'
    WHEN 'capsule' THEN 'CAPSULE'
    WHEN 'ointment' THEN 'OINTMENT'
    WHEN 'cream' THEN 'CREAM'
    WHEN 'drops' THEN 'DROPS'
    WHEN 'inhaler' THEN 'INHALER'
    ELSE 'INHALER'
  END;

ALTER TABLE public.medicine ADD CONSTRAINT chk_medicine_form
CHECK (form IN ('TABLET', 'SYRUP', 'INJECTION', 'CAPSULE', 'OINTMENT', 'CREAM', 'DROPS', 'INHALER'));

UPDATE public.medicine
SET schedule_type =
  CASE lower(trim(schedule_type::text))
    WHEN 'schedule_h' THEN 'SCHEDULE_H'
    WHEN 'schedule_g' THEN 'SCHEDULE_G'
    WHEN 'schedule_x' THEN 'SCHEDULE_X'
    WHEN 'otc' THEN 'OTC'
    ELSE 'OTC'
  END;

ALTER TABLE public.medicine ADD CONSTRAINT chk_medicine_schedule_type
CHECK (schedule_type IN ('SCHEDULE_H', 'SCHEDULE_G', 'SCHEDULE_X', 'OTC'));

-- 6. pharmacy_sale.sale_type and pharmacy_sale.status
UPDATE public.pharmacy_sale
SET sale_type =
  CASE lower(trim(sale_type::text))
    WHEN 'visit' THEN 'VISIT'
    WHEN 'walkin' THEN 'WALKIN'
    ELSE 'WALKIN'
  END;

ALTER TABLE public.pharmacy_sale ADD CONSTRAINT chk_pharmacy_sale_sale_type
CHECK (sale_type IN ('VISIT', 'WALKIN'));

UPDATE public.pharmacy_sale
SET status =
  CASE lower(trim(status::text))
    WHEN 'draft' THEN 'DRAFT'
    WHEN 'completed' THEN 'COMPLETED'
    WHEN 'cancelled' THEN 'CANCELLED'
    ELSE 'CANCELLED'
  END;

ALTER TABLE public.pharmacy_sale ADD CONSTRAINT chk_pharmacy_sale_status
CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED'));

-- 7. item.unit
UPDATE public.item
SET unit =
  CASE lower(trim(unit::text))
    WHEN 'piece' THEN 'PIECE'
    WHEN 'box' THEN 'BOX'
    WHEN 'strip' THEN 'STRIP'
    WHEN 'bottle' THEN 'BOTTLE'
    WHEN 'pack' THEN 'PACK'
    WHEN 'vial' THEN 'VIAL'
    WHEN 'ampule' THEN 'AMPULE'
    ELSE 'AMPULE'
  END;

ALTER TABLE public.item ADD CONSTRAINT chk_item_unit
CHECK (unit IN ('PIECE', 'BOX', 'STRIP', 'BOTTLE', 'PACK', 'VIAL', 'AMPULE'));

-- 8. invoice_item.source_type
UPDATE public.invoice_item
SET source_type =
  CASE lower(trim(source_type::text))
    WHEN 'appointment' THEN 'APPOINTMENT'
    WHEN 'pharmacy_sale_item' THEN 'PHARMACY_SALE_ITEM'
    WHEN 'lab_order' THEN 'LAB_ORDER'
    WHEN 'correction' THEN 'CORRECTION'
    WHEN 'test' THEN 'TEST'
    ELSE 'TEST'
  END;

ALTER TABLE public.invoice_item ADD CONSTRAINT chk_invoice_item_source_type
CHECK (source_type IN ('APPOINTMENT', 'PHARMACY_SALE_ITEM', 'LAB_ORDER', 'CORRECTION', 'TEST'));

-- 9. refund.source_type
UPDATE public.refund
SET source_type =
  CASE lower(trim(source_type::text))
    WHEN 'appointment' THEN 'APPOINTMENT'
    WHEN 'pharmacy_sale_item' THEN 'PHARMACY_SALE_ITEM'
    WHEN 'lab_order' THEN 'LAB_ORDER'
    WHEN 'correction' THEN 'CORRECTION'
    WHEN 'test' THEN 'TEST'
    ELSE 'TEST'
  END;

ALTER TABLE public.refund ADD CONSTRAINT chk_refund_source_type
CHECK (source_type IN ('APPOINTMENT', 'PHARMACY_SALE_ITEM', 'LAB_ORDER', 'CORRECTION', 'TEST'));

-- 10. audit_log.entity_name
UPDATE public.audit_log
SET entity_name =
  CASE lower(trim(entity_name::text))
    WHEN 'patient' THEN 'PATIENT'
    WHEN 'appointment' THEN 'APPOINTMENT'
    WHEN 'invoice' THEN 'INVOICE'
    WHEN 'payment' THEN 'PAYMENT'
    WHEN 'prescription' THEN 'PRESCRIPTION'
    WHEN 'medicine' THEN 'MEDICINE'
    WHEN 'hospital' THEN 'HOSPITAL'
    WHEN 'user_account' THEN 'USER_ACCOUNT'
    WHEN 'user_contract' THEN 'USER_CONTRACT'
    ELSE 'USER_CONTRACT'
  END;

ALTER TABLE public.audit_log ADD CONSTRAINT chk_audit_log_entity_name
CHECK (entity_name IN ('PATIENT', 'APPOINTMENT', 'INVOICE', 'PAYMENT', 'PRESCRIPTION', 'MEDICINE', 'HOSPITAL', 'USER_ACCOUNT', 'USER_CONTRACT'));
