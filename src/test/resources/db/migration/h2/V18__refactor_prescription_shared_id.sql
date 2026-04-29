-- H2-compatible version of V18
-- This version uses a drop-and-recreate strategy to handle dependencies cleanly

-- 1. Drop dependent table first
DROP TABLE IF EXISTS prescription_item;
DROP TABLE IF EXISTS prescription;

-- 2. Recreate prescription table with the new structure (1:1 with appointment)
CREATE TABLE prescription (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    CONSTRAINT fk_prescription_appointment FOREIGN KEY (id) REFERENCES appointment(id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id)
);

-- 3. Recreate prescription_item table (dependent on prescription)
CREATE TABLE prescription_item (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL,
    medicine_id UUID NOT NULL,
    dosage VARCHAR(255) NOT NULL,
    duration VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_prei_pre FOREIGN KEY (prescription_id) REFERENCES prescription(id) ON DELETE CASCADE,
    CONSTRAINT fk_prei_med FOREIGN KEY (medicine_id) REFERENCES medicine(id)
);

-- 4. Create index for performance
CREATE INDEX idx_prescription_patient ON prescription (patient_id);
CREATE INDEX idx_prescription_doctor ON prescription (doctor_id);
CREATE INDEX idx_prescription_item_prescription ON prescription_item (prescription_id);
