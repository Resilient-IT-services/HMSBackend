CREATE TABLE vitals (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    appointment_id UUID NOT NULL UNIQUE,
    blood_pressure VARCHAR(20),
    heart_rate INTEGER,
    temperature DECIMAL(5,2),
    weight DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_vitals_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id),
    CONSTRAINT fk_vitals_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id)
);
