-- Sequence for Sample ID generation
CREATE SEQUENCE IF NOT EXISTS lab.sample_seq START 1;

-- Update lab_sample table to support new workflow
ALTER TABLE lab.lab_sample ADD COLUMN IF NOT EXISTS sample_number VARCHAR(50) UNIQUE;
ALTER TABLE lab.lab_sample ADD COLUMN IF NOT EXISTS collected_by UUID;
ALTER TABLE lab.lab_sample ADD COLUMN IF NOT EXISTS collected_at TIMESTAMPTZ;

-- Link sample to the specific test line item
ALTER TABLE lab.lab_sample ADD COLUMN IF NOT EXISTS order_test_id UUID REFERENCES lab.lab_order_test(id);

-- Ensure lab_order_test has the correct status tracking
ALTER TABLE lab.lab_order_test ALTER COLUMN status SET DEFAULT 'PENDING';
