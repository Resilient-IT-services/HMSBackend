-- V35__add_prescription_status.sql
-- Add status column to prescription table with default 'PENDING'
ALTER TABLE public.prescription ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Add check constraint for status values
ALTER TABLE public.prescription ADD CONSTRAINT chk_prescription_status 
CHECK (status IN ('PENDING', 'DISPENSED', 'CANCELLED'));
