-- V17__payments_and_allocations.sql
-- 1. Payment Table
CREATE TABLE public.payment (
    id uuid NOT NULL PRIMARY KEY,
    hospital_id uuid NOT NULL REFERENCES public.hospital(id),
    patient_id uuid NOT NULL REFERENCES public.patient(id),
    receipt_number varchar(50) NOT NULL,
    amount numeric(12, 2) NOT NULL,
    payment_mode varchar(20) NOT NULL, -- 'CASH', 'CARD', 'UPI', 'NEFT', 'INSURANCE'
    reference_number varchar(100) NULL,
    status varchar(20) NOT NULL, -- 'SUCCESS', 'FAILED', 'REFUNDED'
    payment_date timestamp NOT NULL,
    collected_by uuid NOT NULL REFERENCES public.user_account(id),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_receipt_number UNIQUE (hospital_id, receipt_number)
);

-- 2. Payment Allocations (The Ledger)
CREATE TABLE public.payment_allocation (
    id uuid NOT NULL PRIMARY KEY,
    payment_id uuid NOT NULL REFERENCES public.payment(id),
    invoice_id uuid NOT NULL REFERENCES public.invoice(id),
    allocated_amount numeric(12, 2) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Unified Refund Table
CREATE TABLE public.refund (
    id uuid NOT NULL PRIMARY KEY,
    hospital_id uuid NOT NULL REFERENCES public.hospital(id),
    payment_id uuid NOT NULL REFERENCES public.payment(id),
    invoice_id uuid NOT NULL REFERENCES public.invoice(id),
    refund_amount numeric(12, 2) NOT NULL,
    reason varchar(500) NOT NULL,
    status varchar(20) NOT NULL, -- 'INITIATED', 'COMPLETED'
    source_type varchar(50) NULL, -- e.g., 'PHARMACY_RETURN'
    source_id uuid NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at timestamp NULL
);

CREATE INDEX idx_refund_payment ON public.refund(payment_id);

-- 4. Trigger to maintain invoice.amount_paid
CREATE OR REPLACE FUNCTION fn_update_invoice_amount_paid()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE public.invoice
        SET amount_paid = amount_paid + NEW.allocated_amount,
            status = CASE 
                WHEN (amount_paid + NEW.allocated_amount) >= total_amount THEN 'PAID'
                WHEN (amount_paid + NEW.allocated_amount) > 0 THEN 'PARTIAL'
                ELSE 'UNPAID'
            END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.invoice_id;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE public.invoice
        SET amount_paid = amount_paid - OLD.allocated_amount,
            status = CASE 
                WHEN (amount_paid - OLD.allocated_amount) >= total_amount THEN 'PAID'
                WHEN (amount_paid - OLD.allocated_amount) > 0 THEN 'PARTIAL'
                ELSE 'UNPAID'
            END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = OLD.invoice_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_invoice_amount_paid
AFTER INSERT OR DELETE ON public.payment_allocation
FOR EACH ROW EXECUTE FUNCTION fn_update_invoice_amount_paid();
