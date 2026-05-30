-- U1001__rollback_schemas.sql
-- This script reverses the modular schema migration and moves all tables back to the public schema.

-- 1. Migrate Core Domain back to public
ALTER TABLE IF EXISTS core.hospital SET SCHEMA public;
ALTER TABLE IF EXISTS core.department SET SCHEMA public;
ALTER TABLE IF EXISTS core.specialization SET SCHEMA public;
ALTER TABLE IF EXISTS core.hospital_departments SET SCHEMA public;

-- 2. Migrate IAM Domain back to public
ALTER TABLE IF EXISTS iam.user_account SET SCHEMA public;
ALTER TABLE IF EXISTS iam."role" SET SCHEMA public;
ALTER TABLE IF EXISTS iam.user_contract SET SCHEMA public;
ALTER TABLE IF EXISTS iam.user_contract_department SET SCHEMA public;

-- 3. Migrate Staff Domain back to public
ALTER TABLE IF EXISTS staff.doctor_profile SET SCHEMA public;
ALTER TABLE IF EXISTS staff.nurse_profile SET SCHEMA public;
ALTER TABLE IF EXISTS staff.pharmacist_profile SET SCHEMA public;
ALTER TABLE IF EXISTS staff.receptionist_profile SET SCHEMA public;
ALTER TABLE IF EXISTS staff.lab_tech_profile SET SCHEMA public;
ALTER TABLE IF EXISTS staff.doctor_specialization SET SCHEMA public;
ALTER TABLE IF EXISTS staff.doctor_hospital_details SET SCHEMA public;
ALTER TABLE IF EXISTS staff.nurse_hospital_details SET SCHEMA public;
ALTER TABLE IF EXISTS staff.pharmacist_hospital_details SET SCHEMA public;
ALTER TABLE IF EXISTS staff.receptionist_hospital_details SET SCHEMA public;
ALTER TABLE IF EXISTS staff.admin_hospital_details SET SCHEMA public;
ALTER TABLE IF EXISTS staff.doctor_schedules SET SCHEMA public;
ALTER TABLE IF EXISTS staff.doctor_availability_exception SET SCHEMA public;

-- 4. Migrate Patient Domain back to public
ALTER TABLE IF EXISTS patient.patient SET SCHEMA public;
ALTER TABLE IF EXISTS patient.patient_visit SET SCHEMA public;
ALTER TABLE IF EXISTS patient.vitals SET SCHEMA public;

-- 5. Migrate Appointment Domain back to public
ALTER TABLE IF EXISTS appointment.appointment SET SCHEMA public;

-- 6. Migrate Clinical Domain back to public
ALTER TABLE IF EXISTS clinical.prescription SET SCHEMA public;
ALTER TABLE IF EXISTS clinical.prescription_item SET SCHEMA public;

-- 7. Migrate Billing Domain back to public
ALTER TABLE IF EXISTS billing.billing SET SCHEMA public;
ALTER TABLE IF EXISTS billing.billing_item SET SCHEMA public;
ALTER TABLE IF EXISTS billing.invoice SET SCHEMA public;
ALTER TABLE IF EXISTS billing.invoice_item SET SCHEMA public;
ALTER TABLE IF EXISTS billing.payment SET SCHEMA public;
ALTER TABLE IF EXISTS billing.payment_allocation SET SCHEMA public;
ALTER TABLE IF EXISTS billing.refund SET SCHEMA public;

-- 8. Migrate Pharmacy Domain back to public
ALTER TABLE IF EXISTS pharmacy.medicine SET SCHEMA public;
ALTER TABLE IF EXISTS pharmacy.hospital_medicine_catalog SET SCHEMA public;
ALTER TABLE IF EXISTS pharmacy.pharmacy_stock SET SCHEMA public;
ALTER TABLE IF EXISTS pharmacy.pharmacy_sale SET SCHEMA public;
ALTER TABLE IF EXISTS pharmacy.pharmacy_sale_item SET SCHEMA public;
ALTER TABLE IF EXISTS pharmacy.pharmacy_return SET SCHEMA public;
ALTER TABLE IF EXISTS pharmacy.hsn_master SET SCHEMA public;

-- 9. Migrate Inventory Domain back to public
ALTER TABLE IF EXISTS inventory.item SET SCHEMA public;
ALTER TABLE IF EXISTS inventory.inventory_stock SET SCHEMA public;
ALTER TABLE IF EXISTS inventory.inventory_transaction SET SCHEMA public;

-- 10. Migrate Platform Domain back to public
ALTER TABLE IF EXISTS platform.audit_log SET SCHEMA public;
ALTER TABLE IF EXISTS platform.outbox_event SET SCHEMA public;
ALTER TABLE IF EXISTS platform.vital_range_configs SET SCHEMA public;

-- 11. Optionally drop schemas if they are empty
-- DROP SCHEMA IF EXISTS core;
-- DROP SCHEMA IF EXISTS iam;
-- DROP SCHEMA IF EXISTS staff;
-- DROP SCHEMA IF EXISTS patient;
-- DROP SCHEMA IF EXISTS appointment;
-- DROP SCHEMA IF EXISTS clinical;
-- DROP SCHEMA IF EXISTS billing;
-- DROP SCHEMA IF EXISTS pharmacy;
-- DROP SCHEMA IF EXISTS inventory;
-- DROP SCHEMA IF EXISTS platform;
