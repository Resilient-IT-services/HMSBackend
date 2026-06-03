-- Increase length of blood_group column to accommodate longer enum values like AB_POSITIVE/AB_NEGATIVE
ALTER TABLE patient.patient ALTER COLUMN blood_group TYPE varchar(20);
