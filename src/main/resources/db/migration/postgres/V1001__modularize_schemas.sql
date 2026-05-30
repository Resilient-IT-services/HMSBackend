-- V1001__modularize_schemas.sql
-- 1. Create Bounded Context Schemas
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS staff;
CREATE SCHEMA IF NOT EXISTS patient;
CREATE SCHEMA IF NOT EXISTS appointment;
CREATE SCHEMA IF NOT EXISTS clinical;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS pharmacy;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS platform;

-- 2. Migrate Core Domain
ALTER TABLE IF EXISTS public.hospital SET SCHEMA core;
ALTER TABLE IF EXISTS public.department SET SCHEMA core;
ALTER TABLE IF EXISTS public.specialization SET SCHEMA core;
ALTER TABLE IF EXISTS public.hospital_departments SET SCHEMA core;

-- 3. Migrate IAM Domain
ALTER TABLE IF EXISTS public.user_account SET SCHEMA iam;
ALTER TABLE IF EXISTS public."role" SET SCHEMA iam;
ALTER TABLE IF EXISTS public.user_contract SET SCHEMA iam;
ALTER TABLE IF EXISTS public.user_contract_department SET SCHEMA iam;

-- 4. Migrate Staff Domain
ALTER TABLE IF EXISTS public.doctor_profile SET SCHEMA staff;
ALTER TABLE IF EXISTS public.nurse_profile SET SCHEMA staff;
ALTER TABLE IF EXISTS public.pharmacist_profile SET SCHEMA staff;
ALTER TABLE IF EXISTS public.receptionist_profile SET SCHEMA staff;
ALTER TABLE IF EXISTS public.lab_tech_profile SET SCHEMA staff;
ALTER TABLE IF EXISTS public.doctor_specialization SET SCHEMA staff;
ALTER TABLE IF EXISTS public.doctor_hospital_details SET SCHEMA staff;
ALTER TABLE IF EXISTS public.nurse_hospital_details SET SCHEMA staff;
ALTER TABLE IF EXISTS public.pharmacist_hospital_details SET SCHEMA staff;
ALTER TABLE IF EXISTS public.receptionist_hospital_details SET SCHEMA staff;
ALTER TABLE IF EXISTS public.admin_hospital_details SET SCHEMA staff;
ALTER TABLE IF EXISTS public.doctor_schedules SET SCHEMA staff;
ALTER TABLE IF EXISTS public.doctor_availability_exception SET SCHEMA staff;

-- 5. Migrate Patient Domain
ALTER TABLE IF EXISTS public.patient SET SCHEMA patient;
ALTER TABLE IF EXISTS public.patient_visit SET SCHEMA patient;
ALTER TABLE IF EXISTS public.vitals SET SCHEMA patient;

-- 6. Migrate Appointment Domain
ALTER TABLE IF EXISTS public.appointment SET SCHEMA appointment;

-- 7. Migrate Clinical Domain
ALTER TABLE IF EXISTS public.prescription SET SCHEMA clinical;
ALTER TABLE IF EXISTS public.prescription_item SET SCHEMA clinical;

-- 8. Migrate Billing Domain
ALTER TABLE IF EXISTS public.billing SET SCHEMA billing;
ALTER TABLE IF EXISTS public.billing_item SET SCHEMA billing;
ALTER TABLE IF EXISTS public.invoice SET SCHEMA billing;
ALTER TABLE IF EXISTS public.invoice_item SET SCHEMA billing;
ALTER TABLE IF EXISTS public.payment SET SCHEMA billing;
ALTER TABLE IF EXISTS public.payment_allocation SET SCHEMA billing;
ALTER TABLE IF EXISTS public.refund SET SCHEMA billing;

-- 9. Migrate Pharmacy Domain
ALTER TABLE IF EXISTS public.medicine SET SCHEMA pharmacy;
ALTER TABLE IF EXISTS public.hospital_medicine_catalog SET SCHEMA pharmacy;
ALTER TABLE IF EXISTS public.pharmacy_stock SET SCHEMA pharmacy;
ALTER TABLE IF EXISTS public.pharmacy_sale SET SCHEMA pharmacy;
ALTER TABLE IF EXISTS public.pharmacy_sale_item SET SCHEMA pharmacy;
ALTER TABLE IF EXISTS public.pharmacy_return SET SCHEMA pharmacy;
ALTER TABLE IF EXISTS public.hsn_master SET SCHEMA pharmacy;

-- 10. Migrate Inventory Domain
ALTER TABLE IF EXISTS public.item SET SCHEMA inventory;
ALTER TABLE IF EXISTS public.inventory_stock SET SCHEMA inventory;
ALTER TABLE IF EXISTS public.inventory_transaction SET SCHEMA inventory;

-- 11. Migrate Platform Domain
ALTER TABLE IF EXISTS public.audit_log SET SCHEMA platform;
ALTER TABLE IF EXISTS public.outbox_event SET SCHEMA platform;
ALTER TABLE IF EXISTS public.vital_range_configs SET SCHEMA platform;
