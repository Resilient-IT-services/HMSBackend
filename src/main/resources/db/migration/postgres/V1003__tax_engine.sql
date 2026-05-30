-- V1003__tax_engine.sql
-- Create Tax Engine Schema and Tables

CREATE SCHEMA IF NOT EXISTS tax;

-- 1. Tax Master
CREATE TABLE tax.tax_master (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    tax_type VARCHAR(20) NOT NULL,
    rate NUMERIC(5, 2) NOT NULL,
    is_active BOOLEAN DEFAULT true NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Tax Group
CREATE TABLE tax.tax_group (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. Tax Group Details
CREATE TABLE tax.tax_group_detail (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES tax.tax_group(id) ON DELETE CASCADE,
    tax_master_id UUID NOT NULL REFERENCES tax.tax_master(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 4. Tax Rules
CREATE TABLE tax.tax_rule (
    id UUID PRIMARY KEY,
    priority INTEGER DEFAULT 0 NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    hsn_code VARCHAR(50),
    sac_code VARCHAR(50),
    patient_category VARCHAR(50),
    tax_group_id UUID NOT NULL REFERENCES tax.tax_group(id),
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 5. Snapshot Tables

-- Add tax totals to appointment charge
ALTER TABLE appointment.appointment_charge ADD COLUMN taxable_amount NUMERIC(12, 2) DEFAULT 0 NOT NULL;
ALTER TABLE appointment.appointment_charge ADD COLUMN tax_amount NUMERIC(12, 2) DEFAULT 0 NOT NULL;

ALTER TABLE billing.invoice_item ADD COLUMN taxable_amount NUMERIC(12, 2) DEFAULT 0 NOT NULL;
ALTER TABLE pharmacy.pharmacy_sale_item ADD COLUMN taxable_amount NUMERIC(12, 2) DEFAULT 0 NOT NULL;

ALTER TABLE pharmacy.medicine ADD COLUMN hsn_code VARCHAR(50);

-- Appointment Charge Tax Snapshot
CREATE TABLE appointment.appointment_charge_tax (
    id UUID PRIMARY KEY,
    charge_id UUID NOT NULL REFERENCES appointment.appointment_charge(id) ON DELETE CASCADE,
    tax_code VARCHAR(50) NOT NULL,
    tax_name VARCHAR(100) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL,
    taxable_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Pharmacy Sale Item Tax Snapshot
CREATE TABLE pharmacy.pharmacy_sale_item_tax (
    id UUID PRIMARY KEY,
    pharmacy_sale_item_id UUID NOT NULL REFERENCES pharmacy.pharmacy_sale_item(id) ON DELETE CASCADE,
    tax_code VARCHAR(50) NOT NULL,
    tax_name VARCHAR(100) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL,
    taxable_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Invoice Item Tax Snapshot
CREATE TABLE billing.invoice_item_tax (
    id UUID PRIMARY KEY,
    invoice_item_id UUID NOT NULL REFERENCES billing.invoice_item(id) ON DELETE CASCADE,
    tax_code VARCHAR(50) NOT NULL,
    tax_name VARCHAR(100) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL,
    taxable_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
