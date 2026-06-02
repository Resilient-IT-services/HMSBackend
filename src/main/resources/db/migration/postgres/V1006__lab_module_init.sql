CREATE SCHEMA IF NOT EXISTS lab;

CREATE TABLE lab.lab_department (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_test (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    test_code VARCHAR(50) UNIQUE NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    short_name VARCHAR(100),
    alias_name VARCHAR(255),
    department_id UUID NOT NULL REFERENCES lab.lab_department(id),
    result_type VARCHAR(50),
    tat_value INTEGER,
    tat_unit VARCHAR(20),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_specimen_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50),
    name VARCHAR(100)
);

CREATE TABLE lab.lab_test_specimen (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    specimen_type_id UUID NOT NULL REFERENCES lab.lab_specimen_type(id),
    container_type VARCHAR(100),
    volume_required NUMERIC(10,2),
    fasting_required BOOLEAN,
    collection_instruction TEXT
);

CREATE TABLE lab.lab_instruction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    instruction_type VARCHAR(50),
    instruction_text TEXT
);

CREATE TABLE lab.lab_collection_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    barcode_required BOOLEAN,
    home_collection_allowed BOOLEAN,
    collection_mandatory BOOLEAN
);

CREATE TABLE lab.lab_test_parameter (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    parameter_name VARCHAR(255),
    unit VARCHAR(100),
    display_order INTEGER
);

CREATE TABLE lab.lab_reference_range (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parameter_id UUID NOT NULL REFERENCES lab.lab_test_parameter(id),
    gender VARCHAR(20),
    min_age INTEGER,
    max_age INTEGER,
    age_unit VARCHAR(20),
    min_value NUMERIC(10,2),
    max_value NUMERIC(10,2),
    critical_min NUMERIC(10,2),
    critical_max NUMERIC(10,2)
);

CREATE TABLE lab.lab_package (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    package_code VARCHAR(50),
    package_name VARCHAR(255),
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE lab.lab_package_test (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES lab.lab_package(id),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id)
);

CREATE TABLE lab.lab_price (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    test_id UUID REFERENCES lab.lab_test(id),
    package_id UUID REFERENCES lab.lab_package(id),
    standard_price NUMERIC(12,2),
    emergency_price NUMERIC(12,2),
    insurance_price NUMERIC(12,2),
    effective_from DATE
);

CREATE TABLE lab.lab_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    encounter_id UUID,
    doctor_id UUID,
    order_number VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL,
    ordered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_order_test (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab.lab_order(id),
    test_id UUID REFERENCES lab.lab_test(id),
    package_id UUID REFERENCES lab.lab_package(id),
    status VARCHAR(50) NOT NULL
);

CREATE TABLE lab.lab_order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab.lab_order(id),
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_sample (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    order_test_id UUID NOT NULL REFERENCES lab.lab_order_test(id),
    sample_number VARCHAR(100) UNIQUE NOT NULL,
    specimen_type_id UUID NOT NULL REFERENCES lab.lab_specimen_type(id),
    collected_at TIMESTAMP WITH TIME ZONE,
    collected_by UUID,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE lab.lab_sample_barcode (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sample_id UUID NOT NULL REFERENCES lab.lab_sample(id),
    barcode_value VARCHAR(255) UNIQUE NOT NULL,
    qr_code_value TEXT
);

CREATE TABLE lab.lab_sample_rejection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sample_id UUID NOT NULL REFERENCES lab.lab_sample(id),
    reason TEXT NOT NULL,
    rejected_by UUID,
    rejected_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_machine (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    machine_code VARCHAR(50) UNIQUE NOT NULL,
    machine_name VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(255),
    serial_number VARCHAR(255)
);

CREATE TABLE lab.lab_test_machine (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    machine_id UUID NOT NULL REFERENCES lab.lab_machine(id)
);

CREATE TABLE lab.lab_machine_maintenance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES lab.lab_machine(id),
    maintenance_date DATE NOT NULL,
    next_due_date DATE,
    remarks TEXT
);

CREATE TABLE lab.lab_inventory_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code VARCHAR(50) UNIQUE NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    unit VARCHAR(50)
);

CREATE TABLE lab.lab_inventory_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES lab.lab_inventory_item(id),
    hospital_id UUID NOT NULL,
    available_qty NUMERIC(12,2) DEFAULT 0,
    reorder_level NUMERIC(12,2) DEFAULT 0
);

CREATE TABLE lab.lab_test_consumption_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    item_id UUID NOT NULL REFERENCES lab.lab_inventory_item(id),
    quantity NUMERIC(12,2) NOT NULL
);

CREATE TABLE lab.lab_inventory_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES lab.lab_inventory_item(id),
    transaction_type VARCHAR(50) NOT NULL,
    quantity NUMERIC(12,2) NOT NULL,
    reference_id UUID,
    transaction_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sample_id UUID NOT NULL REFERENCES lab.lab_sample(id),
    entered_by UUID,
    entered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_result_parameter (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL REFERENCES lab.lab_result(id),
    parameter_id UUID NOT NULL REFERENCES lab.lab_test_parameter(id),
    result_value VARCHAR(255),
    abnormal BOOLEAN DEFAULT FALSE,
    critical BOOLEAN DEFAULT FALSE,
    result_status VARCHAR(50) NOT NULL
);

CREATE TABLE lab.lab_critical_alert (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_parameter_id UUID NOT NULL REFERENCES lab.lab_result_parameter(id),
    alert_message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_result_verification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL REFERENCES lab.lab_result(id),
    verified_by UUID,
    verified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    remarks TEXT
);

CREATE TABLE lab.lab_report_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES lab.lab_test(id),
    template_name VARCHAR(255) NOT NULL,
    template_json JSONB
);

CREATE TABLE lab.lab_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab.lab_order(id),
    report_number VARCHAR(100) UNIQUE NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    pdf_path TEXT
);

CREATE TABLE lab.lab_report_delivery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES lab.lab_report(id),
    delivery_method VARCHAR(50) NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lab.lab_referral_lab (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255)
);

CREATE TABLE lab.lab_referral_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_test_id UUID NOT NULL REFERENCES lab.lab_order_test(id),
    referral_lab_id UUID NOT NULL REFERENCES lab.lab_referral_lab(id),
    referred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL
);
