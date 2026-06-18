-- V1028__add_manufacturing_date_to_pharmacy_stock.sql
ALTER TABLE pharmacy.pharmacy_stock ADD COLUMN manufacturing_date DATE;
ALTER TABLE pharmacy.pharmacy_stock_transaction ADD COLUMN manufacturing_date DATE;
