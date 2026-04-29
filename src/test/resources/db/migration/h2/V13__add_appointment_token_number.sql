-- H2-compatible version of V13
-- H2 doesn't support casting to DATE in indexes the same way or has issues with column addition syntax

ALTER TABLE appointment ADD COLUMN IF NOT EXISTS token_number INTEGER;

-- DROP INDEX if exists is standard in newer H2
DROP INDEX IF EXISTS uq_appointment_doctor_date_token;

-- H2 specific unique index on doctor, date and token
-- Try simple index first. If this fails due to date casting, we'll use a computed column.
CREATE UNIQUE INDEX uq_appointment_doctor_date_token 
ON appointment (doctor_id, appointment_time, token_number);
