-- Refactor Doctor and Nurse tables: Rename and use shared ID (user_id -> id)

-- 1. Rename tables
ALTER TABLE doctor RENAME TO doctor_profile;
ALTER TABLE nurse RENAME TO nurse_profile;

-- 2. Drop constraints that depend on old PKs
ALTER TABLE appointment DROP CONSTRAINT fk_apt_dr;
ALTER TABLE prescription DROP CONSTRAINT fk_pre_dr;
ALTER TABLE doctor_availability_exception DROP CONSTRAINT fk_ex_doctor;

-- 3. Update referencing columns in other tables to use user_id instead of old id
UPDATE appointment a SET doctor_id = d.user_id FROM doctor_profile d WHERE a.doctor_id = d.id;
UPDATE prescription p SET doctor_id = d.user_id FROM doctor_profile d WHERE p.doctor_id = d.id;
UPDATE doctor_availability_exception dae SET doctor_id = d.user_id FROM doctor_profile d WHERE dae.doctor_id = d.id;

-- 4. Refactor doctor_profile table (shared ID)
ALTER TABLE doctor_profile DROP CONSTRAINT fk_doctor_user;
ALTER TABLE doctor_profile DROP COLUMN id;
ALTER TABLE doctor_profile RENAME COLUMN user_id TO id;
ALTER TABLE doctor_profile ADD PRIMARY KEY (id);
ALTER TABLE doctor_profile ADD CONSTRAINT fk_doctor_profile_user FOREIGN KEY (id) REFERENCES user_account(id) ON DELETE CASCADE;

-- 5. Refactor nurse_profile table (shared ID)
ALTER TABLE nurse_profile DROP CONSTRAINT fk_nurse_user;
ALTER TABLE nurse_profile DROP COLUMN id;
ALTER TABLE nurse_profile RENAME COLUMN user_id TO id;
ALTER TABLE nurse_profile ADD PRIMARY KEY (id);
ALTER TABLE nurse_profile ADD CONSTRAINT fk_nurse_profile_user FOREIGN KEY (id) REFERENCES user_account(id) ON DELETE CASCADE;

-- 6. Restore constraints with new references
ALTER TABLE appointment ADD CONSTRAINT fk_apt_dr FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id);
ALTER TABLE prescription ADD CONSTRAINT fk_pre_dr FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id);
ALTER TABLE doctor_availability_exception ADD CONSTRAINT fk_ex_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id) ON DELETE CASCADE;
