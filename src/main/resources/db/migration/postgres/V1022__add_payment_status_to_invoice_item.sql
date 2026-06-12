-- V1022__add_payment_status_to_invoice_item.sql
-- Add payment_status column to invoice_item table

ALTER TABLE billing.invoice_item ADD COLUMN payment_status VARCHAR(20) DEFAULT 'UNPAID' NOT NULL;
