-- Migration to support users having multiple roles and linking Doctor/Nurse profiles to UserAccount

-- 1. Add common fields to user_account
ALTER TABLE user_account ADD COLUMN full_name VARCHAR(255);
ALTER TABLE user_account ADD COLUMN gender VARCHAR(20);
ALTER TABLE user_account ADD COLUMN dob DATE;
ALTER TABLE user_account ADD COLUMN phone VARCHAR(20);

-- 2. Create join table for many-to-many roles
CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
);

-- 3. Migrate existing single roles to many-to-many
INSERT INTO user_role (user_id, role_id)
SELECT id, role_id FROM user_account;

-- 4. Drop the old role_id column from user_account
ALTER TABLE user_account DROP COLUMN role_id;

-- 5. Refactor doctor table
-- Add user_id link
ALTER TABLE doctor ADD COLUMN user_id BIGINT;
-- Link existing doctors to users (Assuming one-to-one for existing data if names match, but for a fresh migration we just add the FK)
-- In a real scenario, we'd have logic to pair them. For now, we'll allow it to be nullable initially.
ALTER TABLE doctor ADD CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES user_account(id);

-- Drop redundant fields from doctor
ALTER TABLE doctor DROP COLUMN full_name;
ALTER TABLE doctor DROP COLUMN gender;
ALTER TABLE doctor DROP COLUMN dob;
ALTER TABLE doctor DROP COLUMN phone;

-- 6. Refactor nurse table
-- Add user_id link
ALTER TABLE nurse ADD COLUMN user_id BIGINT;
ALTER TABLE nurse ADD CONSTRAINT fk_nurse_user FOREIGN KEY (user_id) REFERENCES user_account(id);

-- Drop redundant fields from nurse
ALTER TABLE nurse DROP COLUMN full_name;
ALTER TABLE nurse DROP COLUMN gender;
ALTER TABLE nurse DROP COLUMN dob;
ALTER TABLE nurse DROP COLUMN phone;
