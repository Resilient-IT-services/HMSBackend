ALTER TABLE appointment 
ADD COLUMN chief_complaints TEXT,
ADD COLUMN diagnosis TEXT,
ADD COLUMN clinical_notes TEXT,
ADD COLUMN icd_codes JSONB DEFAULT '[]';
