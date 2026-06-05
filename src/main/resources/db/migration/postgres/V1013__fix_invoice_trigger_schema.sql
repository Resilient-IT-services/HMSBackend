-- V1013__fix_invoice_trigger_schema.sql
-- Fix for the trigger function that was still pointing to public.invoice after schema migration

-- 1. Create the fixed function in the billing schema
CREATE OR REPLACE FUNCTION billing.fn_update_invoice_amount_paid()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
    target_invoice_id UUID;
    total_paid NUMERIC(12, 2);
    inv_total_amount NUMERIC(12, 2);
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_invoice_id := OLD.invoice_id;
    ELSE
        target_invoice_id := NEW.invoice_id;
    END IF;

    -- Get total allocated amount for this invoice
    SELECT COALESCE(SUM(allocated_amount), 0) INTO total_paid
    FROM billing.payment_allocation
    WHERE invoice_id = target_invoice_id;

    -- Get the total amount of the invoice to determine status
    SELECT total_amount INTO inv_total_amount
    FROM billing.invoice
    WHERE id = target_invoice_id;

    -- Update the invoice
    UPDATE billing.invoice
    SET amount_paid = total_paid,
        status = CASE 
            WHEN total_paid >= inv_total_amount THEN 'PAID'
            WHEN total_paid > 0 THEN 'PARTIAL'
            ELSE 'UNPAID'
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = target_invoice_id;
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$function$;

-- 2. Drop the old trigger from the table (it might be referencing the old function)
-- Note: V1001 moved payment_allocation to billing schema
DROP TRIGGER IF EXISTS trg_update_invoice_amount_paid ON billing.payment_allocation;

-- 3. Create the new trigger using the fixed function
CREATE TRIGGER trg_update_invoice_amount_paid
AFTER INSERT OR DELETE OR UPDATE ON billing.payment_allocation
FOR EACH ROW EXECUTE FUNCTION billing.fn_update_invoice_amount_paid();

-- 4. Clean up the old function from public schema if it exists
DROP FUNCTION IF EXISTS public.fn_update_invoice_amount_paid();
