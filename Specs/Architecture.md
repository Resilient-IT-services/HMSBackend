# HMS Backend Architecture

Hospital Management System

This document describes the architectural design of the HMS backend system.

The goal is to help developers understand how the healthcare system is structured and how data flows through the application.

---

# 1. Architectural Philosophy

HMS is designed as a **patient-centric event-driven healthcare system**.

Instead of storing only static data, the system records **medical and operational events** and derives insights from them.

This approach ensures:

• consistency
• traceability
• auditability of medical records
• reliability in healthcare workflows

Derived values include:

• Patient Medical History
• Billing Summary
• Resource Utilization
• Hospital Revenue
• Treatment Timeline

---

# 2. High Level System Architecture

Frontend (Web / Mobile)
|
| REST APIs
|
Spring Boot Backend
|
|
Relational Database (PostgreSQL / MySQL)

External systems may integrate via APIs:

• Insurance providers
• Diagnostic labs
• Pharmacy systems

---

# 3. Backend Module Architecture

The backend follows a **modular monolith architecture**.

com.hms

security
user
patient
appointment
billing
pharmacy
inventory
ward
analytics

Each module encapsulates:

• controllers
• services
• repositories
• domain logic

---

# 4. Core Domain Model

The system revolves around patient lifecycle management.

User
|
| 1..*
|
Patient
|
├── Appointment
|
├── Prescription
|
├── Billing
|
└── MedicalRecord

---

# 5. Patient Lifecycle System

Every patient interaction is recorded as an event.

Examples:

• registration
• appointment booking
• consultation
• prescription
• admission
• discharge

Example event:

Date: 2026-01-01
Event: Consultation
Doctor: Dr. X
Notes: Fever and cough

All patient insights are derived from these events.

---

# 6. Appointment Engine

Appointments control doctor scheduling.

Rules:

• no double booking
• time-slot validation
• status tracking

Statuses:

SCHEDULED
COMPLETED
CANCELLED

---

# 7. Treatment Engine

Treatment is managed through prescriptions and procedures.

Workflow:

Doctor Consultation
│
▼
Prescription
│
▼
Pharmacy / Tests
│
▼
Follow-up

The system ensures:

• prescriptions linked to appointments
• treatment traceability
• doctor accountability

---

# 8. Billing Engine

Billing is event-driven and derived from services.

Example:

Consultation Fee
Room Charges
Medicine Charges
Test Charges

Final Bill = Sum of all charge events + Tax

Rules:

• billing records are immutable after finalization
• all charges must be traceable
• supports insurance claims

---

# 9. Inventory & Pharmacy Engine

Inventory tracks all medical resources.

Includes:

• medicines
• equipment
• consumables

Rules:

• stock must never go negative
• expiry tracking required

Pharmacy is tightly integrated with prescriptions.

---

# 10. Ward & Resource Management

Manages hospital resources.

Includes:

• beds
• rooms
• ICU units

Rules:

• real-time availability tracking
• allocation per patient
• discharge updates capacity

---

# 11. Analytics Engine

Analytics services compute hospital insights.

Examples:

Revenue

Revenue = Total Billing - Expenses

Patient Load

Total Patients per day / department

Resource Utilization

Bed Occupancy Rate

---

# 12. Security Architecture

Authentication uses JWT tokens.

Authorization uses role-based access control.

Roles:

ROLE_ADMIN
ROLE_DOCTOR
ROLE_NURSE
ROLE_STAFF

Rules:

• all APIs must be secured
• access must be role-based
• sensitive data must be protected

---

# 13. API Architecture

API endpoints are domain-driven.

Examples:

/api/patients
/api/appointments
/api/billing
/api/pharmacy
/api/inventory
/api/analytics

Rules:

• controllers must remain thin
• services contain business logic
• APIs return DTOs only

---

# 14. Database Design Principles

The database follows these principles:

• medical records are immutable
• billing data is traceable
• relationships are normalized
• audit logs must be maintained

---

# 15. Migration Strategy

Database schema changes are managed using Flyway.

Location:

src/main/resources/db/migration

Example migrations:

V1__create_patient_tables.sql
V2__create_appointment_tables.sql
V3__create_billing_tables.sql

---

# 16. Future Architecture Evolution

Future phases may include:

• AI-based diagnosis support
• telemedicine integration
• IoT device integration
• advanced analytics
• insurance automation

These are excluded from Phase 1 to maintain focus.

---

# 17. Key Design Principles

Developers must follow:

• patient data must be accurate
• records must be traceable
• business logic must be in services
• APIs must be secure

---

# 18. Development Mindset

HMS is not just a CRUD system.

It is a **critical healthcare system**.

Developers must prioritize:

• correctness
• safety
• scalability
• clarity of domain logicS

---

# 🚨 Final Note

Mistakes in this system can impact real patients.

Always write code that is:

• safe
• predictable
• maintainable

---
