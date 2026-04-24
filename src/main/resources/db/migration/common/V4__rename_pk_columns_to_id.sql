-- Rename PK columns to 'id' for consistency across all entities

-- pharmacist_profile: user_id -> id
ALTER TABLE pharmacist_profile RENAME COLUMN user_id TO id;

-- lab_tech_profile: user_id -> id
ALTER TABLE lab_tech_profile RENAME COLUMN user_id TO id;

-- receptionist_profile: user_id -> id
ALTER TABLE receptionist_profile RENAME COLUMN user_id TO id;

-- admin_hospital_details: assignment_id -> id
ALTER TABLE admin_hospital_details RENAME COLUMN assignment_id TO id;

-- doctor_hospital_details: assignment_id -> id
ALTER TABLE doctor_hospital_details RENAME COLUMN assignment_id TO id;

-- nurse_hospital_details: assignment_id -> id
ALTER TABLE nurse_hospital_details RENAME COLUMN assignment_id TO id;

-- pharmacist_hospital_details: assignment_id -> id
ALTER TABLE pharmacist_hospital_details RENAME COLUMN assignment_id TO id;

-- receptionist_hospital_details: assignment_id -> id
ALTER TABLE receptionist_hospital_details RENAME COLUMN assignment_id TO id;
