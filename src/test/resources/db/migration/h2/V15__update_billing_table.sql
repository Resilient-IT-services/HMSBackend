-- H2-compatible version of V15
-- H2 doesn't support multiple ADD COLUMN in one statement
ALTER TABLE billing ADD COLUMN appointment_id UUID;
ALTER TABLE billing ADD CONSTRAINT fk_billing_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id);
ALTER TABLE billing ADD COLUMN discount NUMERIC(12, 2) DEFAULT 0.00;
ALTER TABLE billing ADD COLUMN payment_method VARCHAR(50);

CREATE INDEX idx_billing_appointment ON billing (appointment_id);
