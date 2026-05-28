-- V36__add_hsn_master_and_tax_fields.sql

-- 1. Add HSN fields to medicine table
ALTER TABLE public.medicine ADD COLUMN IF NOT EXISTS hsn_code varchar(50);
ALTER TABLE public.medicine ADD COLUMN IF NOT EXISTS tax_percentage numeric(5, 2) DEFAULT 0;

-- 2. Add HSN fields to invoice_item table
ALTER TABLE public.invoice_item ADD COLUMN IF NOT EXISTS hsn_code varchar(50);

-- 3. Create HSN Master table
CREATE TABLE public.hsn_master (
    id uuid NOT NULL PRIMARY KEY,
    hospital_id uuid NOT NULL REFERENCES public.hospital(id),
    hsn_code varchar(50) NOT NULL,
    description varchar(255),
    tax_percentage numeric(5, 2) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hsn_master_hosp_code ON public.hsn_master(hospital_id, hsn_code);
