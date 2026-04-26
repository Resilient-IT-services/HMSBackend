-- Update day_of_week constraint to allow 1-7 (Monday-Sunday) to match Java/Frontend
ALTER TABLE doctor_schedules DROP CONSTRAINT IF EXISTS doctor_schedules_day_of_week_check;
ALTER TABLE doctor_schedules ADD CONSTRAINT doctor_schedules_day_of_week_check CHECK (day_of_week BETWEEN 1 AND 7);

-- Migrate any existing 0 (Sunday) to 7 if necessary
UPDATE doctor_schedules SET day_of_week = 7 WHERE day_of_week = 0;
