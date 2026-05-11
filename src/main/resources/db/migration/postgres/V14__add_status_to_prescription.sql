-- Add status column to prescription table
ALTER TABLE prescription ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';
