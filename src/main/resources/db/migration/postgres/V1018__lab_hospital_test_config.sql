CREATE TABLE lab.lab_hospital_test_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    active BOOLEAN DEFAULT TRUE,
    tat_value INTEGER,
    tat_unit VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hospital_id, test_id)
);

CREATE INDEX idx_lab_hosp_test_cfg_hosp ON lab.lab_hospital_test_config(hospital_id);
