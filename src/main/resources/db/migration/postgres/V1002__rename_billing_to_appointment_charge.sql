-- V1002__rename_billing_to_appointment_charge.sql
-- Move and rename legacy billing tables to appointment schema to track business-side charges

-- 1. Move tables to appointment schema
ALTER TABLE IF EXISTS billing.billing SET SCHEMA appointment;
ALTER TABLE IF EXISTS billing.billing_item SET SCHEMA appointment;

-- 2. Rename tables
ALTER TABLE appointment.billing RENAME TO appointment_charge;
ALTER TABLE appointment.billing_item RENAME TO appointment_charge_item;

-- 3. Add invoice_id to link with finance module
ALTER TABLE appointment.appointment_charge ADD COLUMN invoice_id UUID;

-- 4. Rename foreign key column in item table for consistency
ALTER TABLE appointment.appointment_charge_item RENAME COLUMN billing_id TO charge_id;

-- 5. Drop and recreate constraints to ensure correct schema references (especially for H2/Postgres compatibility)
-- Drop existing constraints (Drop child table constraints first)
ALTER TABLE appointment.appointment_charge_item DROP CONSTRAINT IF EXISTS billing_item_pkey;
ALTER TABLE appointment.appointment_charge_item DROP CONSTRAINT IF EXISTS fk_billi_bill;
ALTER TABLE appointment.appointment_charge_item DROP CONSTRAINT IF EXISTS fk_billi_hosp;

ALTER TABLE appointment.appointment_charge DROP CONSTRAINT IF EXISTS billing_pkey;
ALTER TABLE appointment.appointment_charge DROP CONSTRAINT IF EXISTS billing_appointment_id_fkey;
ALTER TABLE appointment.appointment_charge DROP CONSTRAINT IF EXISTS fk_bill_hosp;
ALTER TABLE appointment.appointment_charge DROP CONSTRAINT IF EXISTS fk_bill_pat;

-- Recreate constraints with correct schema references
ALTER TABLE appointment.appointment_charge ADD CONSTRAINT appointment_charge_pkey PRIMARY KEY (id);
ALTER TABLE appointment.appointment_charge ADD CONSTRAINT fk_apt_charge_appointment FOREIGN KEY (appointment_id) REFERENCES appointment.appointment(id);
ALTER TABLE appointment.appointment_charge ADD CONSTRAINT fk_apt_charge_hosp FOREIGN KEY (hospital_id) REFERENCES core.hospital(id);
ALTER TABLE appointment.appointment_charge ADD CONSTRAINT fk_apt_charge_pat FOREIGN KEY (patient_id) REFERENCES patient.patient(id);

ALTER TABLE appointment.appointment_charge_item ADD CONSTRAINT appointment_charge_item_pkey PRIMARY KEY (id);
ALTER TABLE appointment.appointment_charge_item ADD CONSTRAINT fk_apt_charge_item_charge FOREIGN KEY (charge_id) REFERENCES appointment.appointment_charge(id) ON DELETE CASCADE;
ALTER TABLE appointment.appointment_charge_item ADD CONSTRAINT fk_apt_charge_item_hosp FOREIGN KEY (hospital_id) REFERENCES core.hospital(id);

-- 6. Update indexes
DROP INDEX IF EXISTS appointment.idx_billing_appointment;
DROP INDEX IF EXISTS appointment.idx_billi_hospital;

CREATE INDEX idx_apt_charge_appointment ON appointment.appointment_charge(appointment_id);
CREATE INDEX idx_apt_charge_item_hospital ON appointment.appointment_charge_item(hospital_id);
