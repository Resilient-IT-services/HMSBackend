CREATE TABLE clinical.prescription_lab_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    prescription_id UUID NOT NULL REFERENCES clinical.prescription(id),
    test_id UUID NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_presc_lab_prescription_id ON clinical.prescription_lab_item(prescription_id);
