-- Refactor existing doctor_schedules and staff_absences
-- Reuse doctor_schedules as is, maybe just add audit columns if needed or just leave it.
-- Rename staff_absences to doctor_availability_exception and update schema.

DROP TABLE staff_absences;

CREATE TABLE doctor_availability_exception (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    hospital_id UUID NOT NULL,
    exception_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'FULL_DAY_OFF', 'MODIFIED', 'ADDED'
    start_time TIME,
    end_time TIME,
    mode VARCHAR(10),          -- override mode if needed
    slot_duration_minutes INT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ex_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE CASCADE,
    CONSTRAINT fk_ex_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id) ON DELETE CASCADE
);

-- Indices for faster lookups
CREATE INDEX idx_dr_avail_ex_lookup ON doctor_availability_exception (doctor_id, hospital_id, exception_date);
