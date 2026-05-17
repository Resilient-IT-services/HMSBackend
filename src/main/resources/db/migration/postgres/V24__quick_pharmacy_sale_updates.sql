-- V24__quick_pharmacy_sale_updates.sql
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS sale_type varchar(30) DEFAULT 'VISIT';
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS external_doctor_name varchar(255);
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS customer_name varchar(255);
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS customer_phone varchar(20);
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS invoice_id uuid;
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS sale_number varchar(50);
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS status varchar(30) DEFAULT 'COMPLETED';
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS doctor_id uuid;
ALTER TABLE pharmacy_sale ADD COLUMN IF NOT EXISTS notes text;

ALTER TABLE pharmacy_sale_item ADD COLUMN IF NOT EXISTS discount_amount numeric(12, 2) DEFAULT 0;
ALTER TABLE pharmacy_sale_item ADD COLUMN IF NOT EXISTS tax_amount numeric(12, 2) DEFAULT 0;
ALTER TABLE pharmacy_sale_item ADD COLUMN IF NOT EXISTS net_amount numeric(12, 2) DEFAULT 0;

-- Support Walk-in sales without patient registration
ALTER TABLE public.invoice ALTER COLUMN patient_id DROP NOT NULL;
ALTER TABLE public.payment ALTER COLUMN patient_id DROP NOT NULL;
