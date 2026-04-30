ALTER TABLE medicine ADD COLUMN IF NOT EXISTS generic_name varchar(255);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS category varchar(100);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS reorder_level int4 DEFAULT 0;
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS selling_price numeric(12, 2);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS purchase_price numeric(12, 2);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS tax_percentage numeric(5, 2);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS storage_temperature varchar(50);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS storage_humidity varchar(50);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS special_notes text;
