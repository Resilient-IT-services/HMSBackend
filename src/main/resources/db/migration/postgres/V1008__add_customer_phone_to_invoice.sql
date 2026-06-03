-- Add customer_phone column to billing.invoice table
ALTER TABLE billing.invoice ADD COLUMN customer_phone varchar(20);
