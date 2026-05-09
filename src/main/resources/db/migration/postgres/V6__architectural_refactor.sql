-- V6__architectural_refactor.sql
-- 1. FIX TENANT ISOLATION: Add hospital_id to missing clinical tables
ALTER TABLE public.prescription ADD COLUMN hospital_id uuid;
ALTER TABLE public.prescription_item ADD COLUMN hospital_id uuid;
ALTER TABLE public.vitals ADD COLUMN hospital_id uuid;

-- Backfill hospital_id from appointment context
-- Prescription and Vitals share the same ID as Appointment (1:1 relationship via MapsId)
UPDATE public.prescription p SET hospital_id = a.hospital_id 
FROM public.appointment a WHERE p.id = a.id;

UPDATE public.vitals v SET hospital_id = a.hospital_id 
FROM public.appointment a WHERE v.id = a.id;

-- PrescriptionItems get hospital_id from their parent Prescription
UPDATE public.prescription_item pi SET hospital_id = p.hospital_id
FROM public.prescription p WHERE pi.prescription_id = p.id;

-- Enforce constraints
ALTER TABLE public.prescription ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE public.prescription ADD CONSTRAINT fk_pre_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id);

ALTER TABLE public.prescription_item ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE public.prescription_item ADD CONSTRAINT fk_prei_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id);

ALTER TABLE public.vitals ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE public.vitals ADD CONSTRAINT fk_vit_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id);

-- 2. GLOBAL MASTER DATA SEPARATION: Create Hospital Medicine Catalog
CREATE TABLE public.hospital_medicine_catalog (
    id uuid NOT NULL,
    hospital_id uuid NOT NULL,
    medicine_id uuid NOT NULL,
    reorder_level int4 DEFAULT 0,
    base_selling_price numeric(12, 2),
    tax_percentage numeric(5, 2),
    is_active bool DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT hospital_medicine_catalog_pkey PRIMARY KEY (id),
    CONSTRAINT uk_hosp_med UNIQUE (hospital_id, medicine_id),
    CONSTRAINT fk_hmc_hosp FOREIGN KEY (hospital_id) REFERENCES public.hospital(id),
    CONSTRAINT fk_hmc_med FOREIGN KEY (medicine_id) REFERENCES public.medicine(id)
);

-- Backfill from global medicine table to maintain existing prices per hospital
-- For existing data, we assume the medicine prices apply to all hospitals currently registered
INSERT INTO public.hospital_medicine_catalog (id, hospital_id, medicine_id, reorder_level, base_selling_price, tax_percentage, created_at, updated_at)
SELECT gen_random_uuid(), h.id, m.id, m.reorder_level, m.selling_price, m.tax_percentage, NOW(), NOW()
FROM public.medicine m
CROSS JOIN public.hospital h
WHERE m.selling_price IS NOT NULL OR m.reorder_level > 0;

-- 3. CONCURRENCY: Add versioning to inventory_stock
ALTER TABLE public.inventory_stock ADD COLUMN "version" int8 DEFAULT 0 NOT NULL;

-- 4. SOFT DELETE: Add deleted flag to core clinical and master data
ALTER TABLE public.patient ADD COLUMN deleted bool DEFAULT false NOT NULL;
ALTER TABLE public.appointment ADD COLUMN deleted bool DEFAULT false NOT NULL;
ALTER TABLE public.prescription ADD COLUMN deleted bool DEFAULT false NOT NULL;
ALTER TABLE public.medicine ADD COLUMN deleted bool DEFAULT false NOT NULL;
ALTER TABLE public.item ADD COLUMN deleted bool DEFAULT false NOT NULL;
ALTER TABLE public.hospital ADD COLUMN deleted bool DEFAULT false NOT NULL;

-- 5. PERFORMANCE: Suggested Indexes
CREATE INDEX idx_prescription_hospital ON public.prescription (hospital_id);
CREATE INDEX idx_vitals_hospital ON public.vitals (hospital_id);
CREATE INDEX idx_prescription_item_hospital ON public.prescription_item (hospital_id);
CREATE INDEX idx_hmc_hosp_med ON public.hospital_medicine_catalog (hospital_id, medicine_id);

-- 6. AUDIT: Add indexes to audit log for performance
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON public.audit_log (entity_name, entity_id);
