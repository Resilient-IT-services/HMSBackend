ALTER TABLE appointment ADD COLUMN session_period VARCHAR(20);

ALTER TABLE vitals ADD COLUMN oxygen_level NUMERIC(5, 2);
ALTER TABLE vitals ADD COLUMN temperature_unit VARCHAR(10) DEFAULT 'CELSIUS';
ALTER TABLE vitals ADD COLUMN weight_unit VARCHAR(10) DEFAULT 'KG';
ALTER TABLE vitals ADD COLUMN systolic INTEGER;
ALTER TABLE vitals ADD COLUMN diastolic INTEGER;
-- Optionally keep blood_pressure for now or migrate data later. 
-- The requirement says "Support separate: Systolic, Diastolic"
