ALTER TABLE medicine 
ADD COLUMN generic_name varchar(255);
ALTER TABLE medicine ADD COLUMN category varchar(100);
ALTER TABLE medicine ADD COLUMN reorder_level int4 DEFAULT 0;
ALTER TABLE medicine ADD COLUMN selling_price numeric(12, 2);
ALTER TABLE medicine ADD COLUMN purchase_price numeric(12, 2);
ALTER TABLE medicine ADD COLUMN tax_percentage numeric(5, 2);
ALTER TABLE medicine ADD COLUMN storage_temperature varchar(50);
ALTER TABLE medicine ADD COLUMN storage_humidity varchar(50);
ALTER TABLE medicine ADD COLUMN special_notes text;