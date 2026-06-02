-- V1004__seed_tax_data.sql
-- Prepopulate Taxation Engine with exhaustive Indian GST data for Healthcare (2024-2025)

-- 1. TAX MASTERS (Atomic Components)
-- Standard GST slabs: 0%, 5%, 12%, 18%
INSERT INTO tax.tax_master (id, code, name, tax_type, rate, is_active, effective_from) VALUES
-- 0% Slab
('f1000000-0000-4000-a000-000000000000', 'GST_0', 'GST Exempt 0%', 'OTHER', 0.00, true, '2024-01-01 00:00:00'),

-- 5% Slab
('f1050000-0000-4000-a000-000000000001', 'CGST_2.5', 'Central GST 2.5%', 'CGST', 2.50, true, '2024-01-01 00:00:00'),
('f1050000-0000-4000-a000-000000000002', 'SGST_2.5', 'State GST 2.5%', 'SGST', 2.50, true, '2024-01-01 00:00:00'),
('f1050000-0000-4000-a000-000000000003', 'IGST_5', 'Integrated GST 5%', 'IGST', 5.00, true, '2024-01-01 00:00:00'),

-- 12% Slab
('f1120000-0000-4000-a000-000000000001', 'CGST_6', 'Central GST 6%', 'CGST', 6.00, true, '2024-01-01 00:00:00'),
('f1120000-0000-4000-a000-000000000002', 'SGST_6', 'State GST 6%', 'SGST', 6.00, true, '2024-01-01 00:00:00'),
('f1120000-0000-4000-a000-000000000003', 'IGST_12', 'Integrated GST 12%', 'IGST', 12.00, true, '2024-01-01 00:00:00'),

-- 18% Slab
('f1180000-0000-4000-a000-000000000001', 'CGST_9', 'Central GST 9%', 'CGST', 9.00, true, '2024-01-01 00:00:00'),
('f1180000-0000-4000-a000-000000000002', 'SGST_9', 'State GST 9%', 'SGST', 9.00, true, '2024-01-01 00:00:00'),
('f1180000-0000-4000-a000-000000000003', 'IGST_18', 'Integrated GST 18%', 'IGST', 18.00, true, '2024-01-01 00:00:00'),

-- Other specific taxes
('f1990000-0000-4000-a000-000000000001', 'CESS_1', 'Cess 1%', 'CESS', 1.00, true, '2024-01-01 00:00:00');


-- 2. TAX GROUPS (Bundles)
-- These bundle all possible components. The engine automatically filters CGST/SGST vs IGST.
INSERT INTO tax.tax_group (id, name, is_active) VALUES
('b1000000-0000-4000-b000-000000000000', 'GST 0% (Exempt)', true),
('b1050000-0000-4000-b000-000000000000', 'GST 5% (Reduced)', true),
('b1120000-0000-4000-b000-000000000000', 'GST 12% (Standard)', true),
('b1180000-0000-4000-b000-000000000000', 'GST 18% (Luxury/Supplements)', true);


-- 3. LINK MASTERS TO GROUPS
-- Link 0% Slab
INSERT INTO tax.tax_group_detail (id, group_id, tax_master_id) VALUES
(gen_random_uuid(), 'b1000000-0000-4000-b000-000000000000', 'f1000000-0000-4000-a000-000000000000');

-- Link 5% Slab
INSERT INTO tax.tax_group_detail (id, group_id, tax_master_id) VALUES
(gen_random_uuid(), 'b1050000-0000-4000-b000-000000000000', 'f1050000-0000-4000-a000-000000000001'), -- CGST 2.5
(gen_random_uuid(), 'b1050000-0000-4000-b000-000000000000', 'f1050000-0000-4000-a000-000000000002'), -- SGST 2.5
(gen_random_uuid(), 'b1050000-0000-4000-b000-000000000000', 'f1050000-0000-4000-a000-000000000003'); -- IGST 5

-- Link 12% Slab
INSERT INTO tax.tax_group_detail (id, group_id, tax_master_id) VALUES
(gen_random_uuid(), 'b1120000-0000-4000-b000-000000000000', 'f1120000-0000-4000-a000-000000000001'), -- CGST 6
(gen_random_uuid(), 'b1120000-0000-4000-b000-000000000000', 'f1120000-0000-4000-a000-000000000002'), -- SGST 6
(gen_random_uuid(), 'b1120000-0000-4000-b000-000000000000', 'f1120000-0000-4000-a000-000000000003'); -- IGST 12

-- Link 18% Slab
INSERT INTO tax.tax_group_detail (id, group_id, tax_master_id) VALUES
(gen_random_uuid(), 'b1180000-0000-4000-b000-000000000000', 'f1180000-0000-4000-a000-000000000001'), -- CGST 9
(gen_random_uuid(), 'b1180000-0000-4000-b000-000000000000', 'f1180000-0000-4000-a000-000000000002'), -- SGST 9
(gen_random_uuid(), 'b1180000-0000-4000-b000-000000000000', 'f1180000-0000-4000-a000-000000000003'); -- IGST 18


-- 4. TAX RULES (Auto-mapping Logic)
-- Priority 100: Exempt Healthcare Services (SAC Codes)
INSERT INTO tax.tax_rule (id, priority, item_type, sac_code, hsn_code, tax_group_id, effective_from) VALUES
-- General Consultation
(gen_random_uuid(), 100, 'CONSULTATION', '999311', NULL, 'b1000000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Specialist Services
(gen_random_uuid(), 100, 'PROCEDURE', '999312', NULL, 'b1000000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Nursing/Physio
(gen_random_uuid(), 100, 'SERVICE', '999313', NULL, 'b1000000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Ambulance
(gen_random_uuid(), 100, 'SERVICE', '999314', NULL, 'b1000000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Lab Tests / Imaging
(gen_random_uuid(), 100, 'LAB_TEST', '999316', NULL, 'b1000000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Blood Bank
(gen_random_uuid(), 100, 'PHARMACY', NULL, '3002', 'b1000000-0000-4000-b000-000000000000', '2024-01-01 00:00:00');

-- Priority 80: Reduced Rate (5%) for Vaccines and Specific Life-saving drugs
INSERT INTO tax.tax_rule (id, priority, item_type, sac_code, hsn_code, tax_group_id, effective_from) VALUES
-- Human Vaccines
(gen_random_uuid(), 80, 'MEDICINE', NULL, '3002', 'b1050000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Diagnostic Reagents (3822)
(gen_random_uuid(), 80, 'LAB_TEST', NULL, '3822', 'b1050000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Medical Instruments (Reduced rate for certain items)
(gen_random_uuid(), 80, 'EQUIPMENT', NULL, '9018', 'b1050000-0000-4000-b000-000000000000', '2024-01-01 00:00:00');

-- Priority 50: Standard Medicine rate (12%)
INSERT INTO tax.tax_rule (id, priority, item_type, sac_code, hsn_code, tax_group_id, effective_from) VALUES
-- Most Prescription Medicaments
(gen_random_uuid(), 50, 'MEDICINE', NULL, '3004', 'b1120000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Pharmaceutical goods (Surgical sutures, sterile gels)
(gen_random_uuid(), 50, 'MEDICINE', NULL, '3006', 'b1120000-0000-4000-b000-000000000000', '2024-01-01 00:00:00');

-- Priority 40: High Rate (18%) for Supplements and Disinfectants
INSERT INTO tax.tax_rule (id, priority, item_type, sac_code, hsn_code, tax_group_id, effective_from) VALUES
-- Food/Health Supplements
(gen_random_uuid(), 40, 'MEDICINE', NULL, '2106', 'b1180000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Hand Sanitisers / Disinfectants
(gen_random_uuid(), 40, 'MEDICINE', NULL, '3808', 'b1180000-0000-4000-b000-000000000000', '2024-01-01 00:00:00');

-- Priority 10: Default Fallback Rules
INSERT INTO tax.tax_rule (id, priority, item_type, sac_code, hsn_code, tax_group_id, effective_from) VALUES
-- Default Medicine fallback (12%)
(gen_random_uuid(), 10, 'MEDICINE', NULL, NULL, 'b1120000-0000-4000-b000-000000000000', '2024-01-01 00:00:00'),
-- Default Service fallback (Exempt for healthcare)
(gen_random_uuid(), 10, 'SERVICE', NULL, NULL, 'b1000000-0000-4000-b000-000000000000', '2024-01-01 00:00:00');
