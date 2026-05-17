-- V20__add_doctor_info_to_invoice_item.sql
ALTER TABLE public.invoice_item ADD COLUMN doctor_id uuid;
ALTER TABLE public.invoice_item ADD COLUMN doctor_name varchar(255);
CREATE INDEX idx_inv_item_doctor ON public.invoice_item(doctor_id);
