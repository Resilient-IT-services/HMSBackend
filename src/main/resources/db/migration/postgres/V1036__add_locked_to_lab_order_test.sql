ALTER TABLE lab.lab_order_test
    ADD COLUMN IF NOT EXISTS locked        BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS locked_reason TEXT;
