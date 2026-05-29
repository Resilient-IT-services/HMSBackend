ALTER TABLE appointment
    ADD COLUMN IF NOT EXISTS appointment_date date,
    ADD COLUMN IF NOT EXISTS appointment_slot_time time;

UPDATE appointment
SET appointment_date = COALESCE(appointment_date, appointment_time::date),
    appointment_slot_time = COALESCE(appointment_slot_time, date_trunc('minute', appointment_time)::time)
WHERE appointment_time IS NOT NULL;

ALTER TABLE appointment
    ALTER COLUMN appointment_date SET NOT NULL,
    ALTER COLUMN appointment_slot_time SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_appointment_active_doctor_slot
    ON appointment (hospital_id, doctor_id, appointment_date, appointment_slot_time)
    WHERE deleted = false AND status <> 'CANCELLED';
