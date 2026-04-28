ALTER TABLE prescription ADD COLUMN appointment_id UUID;
ALTER TABLE prescription ADD CONSTRAINT fk_prescription_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id);
CREATE INDEX idx_prescription_appointment ON prescription (appointment_id);
