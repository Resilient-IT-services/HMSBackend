-- V16__unified_financial_architecture.sql
-- 1. Unified Invoice Table
CREATE TABLE public.invoice (
    id uuid NOT NULL PRIMARY KEY,
    hospital_id uuid NOT NULL REFERENCES public.hospital(id),
    patient_id uuid NOT NULL REFERENCES public.patient(id),
    visit_id uuid NULL REFERENCES public.patient_visit(id),
    invoice_number varchar(50) NOT NULL,
    status varchar(20) NOT NULL, -- 'DRAFT', 'UNPAID', 'PARTIAL', 'PAID', 'CANCELLED'
    subtotal_amount numeric(12, 2) NOT NULL DEFAULT 0,
    tax_amount numeric(12, 2) NOT NULL DEFAULT 0,
    discount_amount numeric(12, 2) NOT NULL DEFAULT 0,
    total_amount numeric(12, 2) NOT NULL DEFAULT 0,
    amount_paid numeric(12, 2) NOT NULL DEFAULT 0,
    amount_due numeric(12, 2) GENERATED ALWAYS AS (total_amount - amount_paid) STORED,
    due_date timestamp NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_invoice_number UNIQUE (hospital_id, invoice_number)
);

CREATE INDEX idx_invoice_visit ON public.invoice(visit_id);
CREATE INDEX idx_invoice_patient ON public.invoice(patient_id);
CREATE INDEX idx_invoice_status ON public.invoice(hospital_id, status);

-- 2. Invoice Items (Polymorphic)
CREATE TABLE public.invoice_item (
    id uuid NOT NULL PRIMARY KEY,
    invoice_id uuid NOT NULL REFERENCES public.invoice(id) ON DELETE CASCADE,
    hospital_id uuid NOT NULL REFERENCES public.hospital(id),
    item_description varchar(255) NOT NULL,
    category varchar(50) NOT NULL, -- 'CONSULTATION', 'PHARMACY', 'LAB', 'PROCEDURE'
    quantity numeric(10, 2) NOT NULL,
    unit_price numeric(12, 2) NOT NULL,
    tax_percentage numeric(5, 2) DEFAULT 0,
    tax_amount numeric(12, 2) DEFAULT 0,
    discount_amount numeric(12, 2) DEFAULT 0,
    subtotal numeric(12, 2) NOT NULL,
    net_total numeric(12, 2) NOT NULL,
    source_type varchar(50) NOT NULL, -- 'APPOINTMENT', 'PHARMACY_SALE_ITEM', 'LAB_ORDER'
    source_id uuid NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inv_item_source ON public.invoice_item(source_type, source_id);
