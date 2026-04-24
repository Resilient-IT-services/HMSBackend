-- New hospital-specific detail tables for each role

CREATE TABLE admin_hospital_details (
    assignment_id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    office_location varchar(255),
    is_department_head bool DEFAULT false,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE nurse_hospital_details (
    assignment_id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    floor_location varchar(255),
    shift_type varchar(50), -- e.g., DAY, NIGHT, ROTATIONAL
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pharmacist_hospital_details (
    assignment_id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    counter_location varchar(255),
    pharmacy_wing varchar(100),
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE receptionist_hospital_details (
    assignment_id uuid PRIMARY KEY REFERENCES user_role_assignment(id) ON DELETE CASCADE,
    desk_location varchar(255),
    emergency_contact_number varchar(20),
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
