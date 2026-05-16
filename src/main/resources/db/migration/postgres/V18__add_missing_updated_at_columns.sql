-- V18__add_missing_updated_at_columns.sql

-- Add missing updated_at columns to tables introduced in V16 and V17
ALTER TABLE public.invoice_item ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.payment ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.payment_allocation ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.refund ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
