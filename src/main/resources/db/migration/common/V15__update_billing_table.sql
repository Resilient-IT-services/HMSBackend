ALTER TABLE billing 
ADD COLUMN appointment_id UUID REFERENCES appointment(id),
ADD COLUMN discount NUMERIC(12, 2) DEFAULT 0.00,
ADD COLUMN payment_method VARCHAR(50);

CREATE INDEX idx_billing_appointment ON billing (appointment_id);
