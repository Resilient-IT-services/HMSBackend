-- V1012__remove_legacy_invoice_item_tax_percentage.sql
-- Remove redundant tax_percentage column from invoice_item
-- This is superseded by the invoice_item_tax snapshot table (introduced in V1003)

ALTER TABLE billing.invoice_item DROP COLUMN IF EXISTS tax_percentage;
