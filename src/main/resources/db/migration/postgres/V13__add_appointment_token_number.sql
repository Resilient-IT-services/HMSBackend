-- Ensure token_number exists as INTEGER
-- Using safe SQL for both H2 and Postgres
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS token_number INTEGER;

-- Unique constraint per doctor, per day, per token
-- Keep this migration portable across PostgreSQL and H2 test runs.
DROP INDEX IF EXISTS uq_appointment_doctor_date_token;
CREATE UNIQUE INDEX uq_appointment_doctor_date_token 
ON appointment (doctor_id, appointment_time, token_number);
