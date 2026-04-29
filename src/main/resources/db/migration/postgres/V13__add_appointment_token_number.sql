-- Ensure token_number exists as INTEGER
-- Using safe SQL for both H2 and Postgres
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS token_number INTEGER;

-- Unique constraint per doctor, per day, per token
-- Functional index for date part
DROP INDEX IF EXISTS uq_appointment_doctor_date_token;
CREATE UNIQUE INDEX uq_appointment_doctor_date_token 
ON appointment (doctor_id, (CAST(appointment_time AS date)), token_number);
