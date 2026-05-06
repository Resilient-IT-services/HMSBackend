CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE medicine DROP CONSTRAINT IF EXISTS fk_med_hosp;
ALTER TABLE medicine DROP COLUMN IF EXISTS hospital_id;
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS strength varchar(100);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS form varchar(100);
ALTER TABLE medicine ADD COLUMN IF NOT EXISTS schedule_type varchar(100);

ALTER TABLE pharmacy_stock RENAME COLUMN price TO selling_price;
ALTER TABLE pharmacy_stock ADD COLUMN IF NOT EXISTS purchase_price numeric(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE pharmacy_stock ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS pharmacy_sale (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    patient_id uuid NULL,
    prescription_id uuid NULL,
    total_amount numeric(12, 2) NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_pharmacy_sale_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);

CREATE TABLE IF NOT EXISTS pharmacy_sale_item (
    id uuid PRIMARY KEY,
    sale_id uuid NOT NULL,
    medicine_id uuid NOT NULL,
    stock_id uuid NOT NULL,
    quantity int4 NOT NULL,
    unit_price numeric(12, 2) NOT NULL DEFAULT 0,
    subtotal numeric(12, 2) NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_pharmacy_sale_item_sale FOREIGN KEY (sale_id) REFERENCES pharmacy_sale(id) ON DELETE CASCADE,
    CONSTRAINT fk_pharmacy_sale_item_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id),
    CONSTRAINT fk_pharmacy_sale_item_stock FOREIGN KEY (stock_id) REFERENCES pharmacy_stock(id)
);

CREATE TABLE IF NOT EXISTS pharmacy_return (
    id uuid PRIMARY KEY,
    hospital_id uuid NOT NULL,
    sale_item_id uuid NOT NULL,
    stock_id uuid NOT NULL,
    quantity int4 NOT NULL,
    reason varchar(500),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_pharmacy_return_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_pharmacy_return_sale_item FOREIGN KEY (sale_item_id) REFERENCES pharmacy_sale_item(id),
    CONSTRAINT fk_pharmacy_return_stock FOREIGN KEY (stock_id) REFERENCES pharmacy_stock(id)
);

CREATE INDEX IF NOT EXISTS idx_medicine_name ON medicine (lower(name));
CREATE INDEX IF NOT EXISTS idx_medicine_generic_name ON medicine (lower(generic_name));
CREATE INDEX IF NOT EXISTS idx_medicine_composition ON medicine (lower(composition));
CREATE INDEX IF NOT EXISTS idx_medicine_search_trgm
    ON medicine USING gin ((coalesce(name, '') || ' ' || coalesce(generic_name, '') || ' ' || coalesce(composition, '')) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_pharmacy_stock_hospital_medicine
    ON pharmacy_stock (hospital_id, medicine_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_stock_fefo
    ON pharmacy_stock (hospital_id, medicine_id, expiry_date, quantity);
CREATE UNIQUE INDEX IF NOT EXISTS uq_pharmacy_stock_batch
    ON pharmacy_stock (hospital_id, medicine_id, batch_number);
