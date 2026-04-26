-- Add city, state, and pincode to hospital table
ALTER TABLE hospital ADD COLUMN city VARCHAR(100);
ALTER TABLE hospital ADD COLUMN state VARCHAR(100);
ALTER TABLE hospital ADD COLUMN pincode VARCHAR(20);
