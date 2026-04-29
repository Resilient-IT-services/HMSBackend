CREATE TABLE vital_settings (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    vital_name VARCHAR(50) NOT NULL,
    min_value NUMERIC(10, 2),
    max_value NUMERIC(10, 2),
    unit VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vital_hospital UNIQUE (hospital_id, vital_name)
);
