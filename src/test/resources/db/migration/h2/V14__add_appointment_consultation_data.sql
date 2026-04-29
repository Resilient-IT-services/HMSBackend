-- H2-compatible version of V14
-- H2 doesn't support multiple ADD COLUMN in one statement and uses JSON instead of JSONB
ALTER TABLE appointment ADD COLUMN chief_complaints TEXT;
ALTER TABLE appointment ADD COLUMN diagnosis TEXT;
ALTER TABLE appointment ADD COLUMN clinical_notes TEXT;
ALTER TABLE appointment ADD COLUMN icd_codes JSON DEFAULT '[]';
