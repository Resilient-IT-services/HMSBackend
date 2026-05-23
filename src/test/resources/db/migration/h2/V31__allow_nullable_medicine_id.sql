-- Support free-text medicines in prescriptions
ALTER TABLE prescription_item ALTER COLUMN medicine_id DROP NOT NULL;
