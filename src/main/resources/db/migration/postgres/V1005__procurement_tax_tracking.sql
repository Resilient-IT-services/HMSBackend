-- Procurement Tax Tracking Tables

CREATE TABLE pharmacy.pharmacy_stock_transaction (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    medicine_id UUID NOT NULL,
    batch_number VARCHAR(255) NOT NULL,
    expiry_date DATE,
    quantity INTEGER NOT NULL,
    purchase_price DECIMAL(12, 2) DEFAULT 0,
    selling_price DECIMAL(12, 2) DEFAULT 0,
    taxable_amount DECIMAL(12, 2) DEFAULT 0,
    tax_amount DECIMAL(12, 2) DEFAULT 0,
    net_amount DECIMAL(12, 2) DEFAULT 0,
    type VARCHAR(50) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference VARCHAR(255),
    supplier_name VARCHAR(255),
    supplier_state VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_pst_hospital FOREIGN KEY (hospital_id) REFERENCES core.hospital(id),
    CONSTRAINT fk_pst_medicine FOREIGN KEY (medicine_id) REFERENCES pharmacy.medicine(id)
);

CREATE TABLE pharmacy.pharmacy_stock_transaction_tax (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    tax_code VARCHAR(50) NOT NULL,
    tax_name VARCHAR(255) NOT NULL,
    tax_rate DECIMAL(5, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) NOT NULL,
    taxable_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_pstt_transaction FOREIGN KEY (transaction_id) REFERENCES pharmacy.pharmacy_stock_transaction(id)
);

CREATE INDEX idx_pst_hospital_date ON pharmacy.pharmacy_stock_transaction(hospital_id, transaction_date);
CREATE INDEX idx_pst_medicine ON pharmacy.pharmacy_stock_transaction(medicine_id);
