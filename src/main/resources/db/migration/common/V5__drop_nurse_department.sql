-- Migration to move nurse department to user_role_assignment
ALTER TABLE nurse DROP COLUMN department;
