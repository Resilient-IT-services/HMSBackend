-- Refactor vitals table to use shared ID with appointment and remove redundant fields
TRUNCATE TABLE vitals;

ALTER TABLE vitals DROP CONSTRAINT fk_vitals_appointment;
ALTER TABLE vitals DROP CONSTRAINT fk_vitals_hospital;
ALTER TABLE vitals DROP COLUMN id;
ALTER TABLE vitals DROP COLUMN hospital_id;
ALTER TABLE vitals DROP COLUMN created_at;
ALTER TABLE vitals DROP COLUMN updated_at;

ALTER TABLE vitals RENAME COLUMN appointment_id TO id;
ALTER TABLE vitals ADD PRIMARY KEY (id);
ALTER TABLE vitals ADD CONSTRAINT fk_vitals_appointment FOREIGN KEY (id) REFERENCES appointment(id) ON DELETE CASCADE;
