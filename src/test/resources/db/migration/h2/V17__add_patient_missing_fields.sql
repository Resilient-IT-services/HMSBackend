-- H2-compatible version of V17
-- H2 doesn't support multiple ADD COLUMN in one statement
ALTER TABLE patient ADD COLUMN email VARCHAR(255);
ALTER TABLE patient ADD COLUMN medical_history TEXT;
ALTER TABLE patient ADD COLUMN blood_group VARCHAR(10);
