# HMS Domain Model (Advanced)

This document describes the **enhanced domain model** for the Hospital Management System (HMS).

It includes realistic healthcare entities, detailed attributes, and relationships required for a production-ready system.

---

# 1. Domain Overview

HMS is structured around a **patient lifecycle + hospital operations model**:

User → Patient → Appointment → Treatment → Billing → Records → Inventory

The system tracks every interaction and ensures traceability of medical and financial data.

---

# 2. Core Entity Relationships

User
|
| 1..*
|
Patient
|
├── Appointment
│      └── Prescription
│             └── PrescriptionItem
│
├── Admission
│      └── Bed
│
├── Billing
│      └── BillingItem
│
└── MedicalRecord

---

# 3. Entities (Detailed)

---

## 3.1 User

Represents system users (Doctor/Admin/Nurse)

Fields:

* id
* username
* email
* password_hash
* role
* specialization (for doctors)
* license_number
* status
* created_at

---

## 3.2 Patient

Fields:

* id
* patient_code (unique hospital ID)
* name
* age
* gender
* blood_group
* contact_number
* emergency_contact
* address
* allergies
* medical_history_summary
* created_at

---

## 3.3 Appointment

Fields:

* id
* patient_id
* doctor_id
* appointment_datetime
* status (SCHEDULED / COMPLETED / CANCELLED)
* consultation_type (OPD / ONLINE / EMERGENCY)
* notes

---

## 3.4 Prescription

Fields:

* id
* appointment_id
* patient_id
* doctor_id
* diagnosis
* instructions
* created_at

---

## 3.5 Prescription Item

Fields:

* id
* prescription_id
* medicine_id
* dosage (e.g., 1-0-1)
* duration_days
* frequency
* instructions

---

## 3.6 Medicine

🔥 (Important – real-world level)

Fields:

* id
* name
* generic_name
* brand_name
* manufacturer
* drug_type (tablet / syrup / injection)
* strength (e.g., 500mg)
* unit (strip / bottle / vial)
* price_per_unit
* requires_prescription (boolean)
* storage_conditions (e.g., keep refrigerated)

---

## 3.7 Medicine Batch

👉 (VERY IMPORTANT for real systems)

Fields:

* id
* medicine_id
* batch_number
* manufacturing_date
* expiry_date
* purchase_price
* selling_price
* quantity_available
* supplier_id

---

## 3.8 Supplier

Fields:

* id
* name
* contact
* address
* license_number

---

## 3.9 Inventory

Fields:

* id
* medicine_batch_id
* quantity_in_stock
* reorder_level
* last_updated

Rules:

• stock tracked per batch
• expiry-aware inventory

---

## 3.10 Pharmacy Transaction

Fields:

* id
* prescription_id
* patient_id
* total_amount
* transaction_date

---

## 3.11 Pharmacy Item

Fields:

* id
* pharmacy_transaction_id
* medicine_batch_id
* quantity
* price
* total

---

## 3.12 Billing

Fields:

* id
* patient_id
* bill_number
* total_amount
* discount
* tax
* final_amount
* payment_status (PAID / PENDING / PARTIAL)
* payment_mode (CASH / CARD / UPI / INSURANCE)
* created_at

---

## 3.13 Billing Item

Fields:

* id
* billing_id
* item_type (CONSULTATION / MEDICINE / ROOM / TEST)
* description
* quantity
* unit_price
* total

---

## 3.14 Admission

Fields:

* id
* patient_id
* ward_id
* bed_id
* admission_date
* discharge_date
* status

---

## 3.15 Ward

Fields:

* id
* ward_name
* ward_type (GENERAL / ICU / PRIVATE)
* total_beds

---

## 3.16 Bed

Fields:

* id
* ward_id
* bed_number
* status (AVAILABLE / OCCUPIED / MAINTENANCE)

---

## 3.17 Medical Record

Fields:

* id
* patient_id
* diagnosis_history
* treatment_history
* lab_reports
* attachments
* last_updated

---

## 3.18 Lab Test

Fields:

* id
* test_name
* category
* price

---

## 3.19 Lab Report

Fields:

* id
* patient_id
* test_id
* result
* report_file
* created_at

---

# 4. Domain Flow

Patient Registration
↓
Appointment
↓
Doctor Consultation
↓
Prescription
↓
Pharmacy Dispense (Batch-based)
↓
Billing
↓
Medical Record Update

---

# 5. Inventory Flow

Medicine Purchase
↓
Batch Created
↓
Inventory Updated
↓
Medicine Dispensed
↓
Stock Reduced

---

# 6. Billing Flow

Consultation

* Medicines
* Tests
* Room Charges
  ↓
  Bill Generated
  ↓
  Payment Processed

---

# 7. Derived Data

Examples:

Hospital Revenue

= Sum of all Billing.final_amount

Stock Alert

= quantity < reorder_level

Bed Occupancy Rate

= occupied beds / total beds

---

# 8. Phase-1 Design Principles

1. Patient data must be consistent
2. Medical history must be traceable
3. Billing must be accurate
4. Inventory must be batch-based
5. Avoid over-complexity in Phase 1

---

# 9. Future Extensions

• Insurance Claims Module
• Doctor Scheduling Optimization
• AI Diagnosis
• Multi-Hospital Support
• IoT Integration

---

# 🚨 FINAL NOTE

This is not a basic CRUD system.

This is a **real-world healthcare system**.

Every entity must ensure:

• data accuracy
• traceability
• safety

---
