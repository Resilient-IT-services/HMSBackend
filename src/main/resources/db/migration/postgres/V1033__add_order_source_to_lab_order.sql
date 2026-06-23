ALTER TABLE lab.lab_order
    ADD COLUMN IF NOT EXISTS order_source VARCHAR(20);
