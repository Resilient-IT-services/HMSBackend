-- V1037__progress_notes.sql
CREATE TABLE ipd.progress_notes (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    admission_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progress_notes_admission FOREIGN KEY (admission_id) REFERENCES ipd.admission(id),
    CONSTRAINT fk_progress_notes_doctor FOREIGN KEY (doctor_id) REFERENCES iam.user_account(id)
);
