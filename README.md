# HMS Phase 1 Scope (Codex Ready)

## 1. Objective
Build a Hospital Management System (HMS) focused on **OPD workflows**, with:
- Patient registration
- Appointment management
- Doctor/Nurse management
- OPD billing
- Pharmacy dispensing
- Role-based access (simple, no RBAC)

---

## 2. Scope (In-Scope)

### 2.1 Authentication
- Cookie-based authentication
- Login / Logout
- Secure HTTP-only cookie

---

### 2.2 User & Roles
- Predefined roles:
  - ADMIN
  - DOCTOR
  - NURSE
  - RECEPTIONIST
  - PHARMACIST
- Create user with role
- Role-based access enforced in backend
- No permission system

---

### 2.3 Patient Management
- Create patient
- View patient list
- View patient details

---

### 2.4 Staff Management

#### Doctor
- Create doctor
- View doctors

#### Nurse
- Create nurse
- View nurses

---

### 2.5 Appointment Management (OPD Only)

- Create appointment (Receptionist)
- View appointments
- Update appointment status

Supported statuses:
- SCHEDULED
- COMPLETED
- NO_SHOW
- DELAYED
- CANCELLED

---

### 2.6 Billing (OPD + Pharmacy Only)

- Create OPD bill
- Add billing items
- Calculate total

#### Pharmacy Billing
- Create pharmacy bill (linked to prescription)
- Auto-add medicine items from dispense
- Maintain per-item price, quantity, subtotal

No payment processing in Phase 1

---

### 2.7 Pharmacy

#### Prescription (Doctor)
- Doctor creates prescription
- Add multiple medicines with dosage and duration

#### Pharmacy Dispensing
- Pharmacist views prescriptions
- Dispense medicines (partial/full)
- Dispense action reduces stock
- Dispense creates/updates pharmacy billing

#### Pharmacy Stock Management
- Maintain stock per medicine + batch
- Track:
  - batch_number
  - expiry_date
  - quantity
  - price
- Support stock add (purchase/entry)
- Support stock deduction (dispense)
- Prevent dispense if insufficient stock
- Alert/flag for near expiry (optional)

---

### 2.8 Inventory

- Add item
- Update stock
- Track stock quantity

---

### 2.9 Audit Logs

- Track key actions (create/update)
- Fetch audit logs

---

## 3. Out of Scope (Phase 1)

- Admission / IPD
- Payment processing
- Online payment gateway
- Advanced RBAC / permissions
- Multi-hospital UI workflows
- Reports & analytics (basic only if needed)

---

## 4. Core Technical Constraints

- Single database
- `hospital_id` enforced at backend
- No trust on frontend
- All APIs must be hospital-scoped

---

## 5. Key Deliverables

- Backend APIs (Spring Boot)
- Database schema
- Authentication (cookie-based)
- Core modules implemented:
  - Patient
  - Appointment
  - Billing
  - Pharmacy
  - Inventory

---

## 6. Development Principles

- No business logic in controllers
- Strict validation at API layer
- Consistent API response structure
- Pagination for all list APIs
- Error handling with standard codes

---

## 7. Success Criteria

- Receptionist can manage appointments
- Doctor can view assigned patients and create prescriptions
- Pharmacy can dispense medicines and generate billing
- Admin can manage users
- Stock is reduced correctly on dispense
- System works fully offline (on-prem ready)

---

## 8. Notes for Codex

- Keep implementation simple (Phase 1 focus)
- Avoid over-engineering
- Follow modular structure
- Design for future extensibility (Person model, multi-hospital ready)

---

## Summary

Phase 1 focuses on **core hospital operations (OPD)** with minimal complexity and strong backend control. The goal is fast delivery with a solid architectural foundation.

Each hospital is identified using a `hospital_id`, which acts as the primary boundary for all data access and operations.

---

## Core Rules

### 1. Single Database
- Only one physical database for all hospitals
- No per-hospital databases or schemas
- All data resides in shared tables
- 70%minimum  test case coverage .desired 90%
- Swagger APi documentation in detail is mandatory.

---

### 2. Tenant Isolation via `hospital_id`
- Each hospital is treated as a tenant
- `hospital_id` is the mandatory partition key
- All queries, writes, and operations must be scoped by `hospital_id`

---

### 3. Mandatory `hospital_id` in Domain Tables
- EVERY domain table must include `hospital_id`
- No exceptions for business tables
- `hospital_id` must be:
    - Non-null
    - Indexed
    - Part of query filters

---

### 4. Backend-Enforced Isolation
- Isolation must be enforced strictly at backend level
- Frontend must never be trusted for tenant filtering

#### Enforcement Requirements
- Extract `hospital_id` from authenticated context (JWT/session)
- Do NOT accept `hospital_id` from request payload
- Automatically inject `hospital_id` into all queries
- Validate access on every request

---

## Non-Negotiable Constraints

- No cross-hospital data access
- No shared mutable data across tenants
- No bypassing of tenant filters
- Any violation is a critical security defect

---

## Design Implications

- All repositories must enforce `hospital_id` filtering
- Composite indexes should include `hospital_id`
- Unique constraints should include `hospital_id` where applicable
- Logging and auditing must include `hospital_id`

---

## Special Case: Global Entities (e.g., Doctors)

While the system enforces strict tenant isolation using `hospital_id`, certain entities may exist globally across hospitals.

### Example: Doctor
- A doctor can be associated with multiple hospitals
- Doctor identity should be stored as a global entity
- Hospital-specific data must NOT be stored in the global table

### Recommended Pattern
- Global Table: `Doctor`
- Mapping Table: `Doctor_Hospital`

### Rules
- All hospital-specific operations must go through mapping tables
- Global tables must NEVER be directly exposed in APIs without hospital context
- Access must always be resolved using `hospital_id`

---

## Summary

This model ensures:
- Strong tenant isolation
- Simplified infrastructure
- Scalable multi-hospital support

The system must behave as if each hospital has its own isolated system, even though all data resides in a shared database.

# HMS Database Schema (Multi-Hospital, Detailed)

## Conventions
- All tables use `hospital_id` unless explicitly marked GLOBAL
- Naming: snake_case
- PK: `id` (BIGINT, auto-increment)
- Timestamps: `created_at`, `updated_at`
- Soft delete: `is_deleted` (BOOLEAN)

---

## 1. Hospital (GLOBAL)

```
hospital
- id BIGINT PK
- name VARCHAR(255) NOT NULL
- code VARCHAR(50) UNIQUE NOT NULL
- address TEXT
- status VARCHAR(20) NOT NULL
- created_at TIMESTAMP
- updated_at TIMESTAMP
```

---

# 2. Database Schema (Refactored)

## 2.1 Person (GLOBAL)
```
person
- id BIGINT PK
- full_name VARCHAR(255) NOT NULL
- gender VARCHAR(10)
- dob DATE
- phone VARCHAR(20)
- email VARCHAR(255)
- created_at TIMESTAMP
- updated_at TIMESTAMP
```

---

## 2.2 Person_Hospital (Mapping)
```
person_hospital
- id BIGINT PK
- person_id BIGINT NOT NULL
- hospital_id BIGINT NOT NULL
- role VARCHAR(50) NOT NULL
- is_active BOOLEAN DEFAULT TRUE
- created_at TIMESTAMP

FK person_id -> person(id)
FK hospital_id -> hospital(id)
UNIQUE (person_id, hospital_id, role)
INDEX (hospital_id)
```

---

## 2.3 Doctor (Extension)
```
doctor
- id BIGINT PK
- person_id BIGINT NOT NULL
- specialization VARCHAR(255)

FK person_id -> person(id)
UNIQUE (person_id)
```

---

## 2.4 Nurse (Extension)
```
nurse
- id BIGINT PK
- person_id BIGINT NOT NULL
- department VARCHAR(100)

FK person_id -> person(id)
UNIQUE (person_id)
```

---

## 2.5 Pharmacist (Extension)
```
pharmacist
- id BIGINT PK
- person_id BIGINT NOT NULL

FK person_id -> person(id)
UNIQUE (person_id)
```

---

## 4. Patient

```
patient
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- full_name VARCHAR(255) NOT NULL
- gender VARCHAR(10)
- dob DATE
- phone VARCHAR(20)
- address TEXT
- created_at TIMESTAMP
- updated_at TIMESTAMP

FK hospital_id -> hospital(id)
INDEX (hospital_id)
```

---

## 5. Appointment

```
appointment
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- patient_id BIGINT NOT NULL
- doctor_hospital_id BIGINT NOT NULL
- appointment_time TIMESTAMP NOT NULL
- status VARCHAR(20)
- notes TEXT
- created_at TIMESTAMP
- updated_at TIMESTAMP

FK hospital_id -> hospital(id)
FK patient_id -> patient(id)
FK doctor_hospital_id -> doctor_hospital(id)
INDEX (hospital_id)
```

---

## 6. Admission

```
admission
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- patient_id BIGINT NOT NULL
- admitted_at TIMESTAMP NOT NULL
- discharged_at TIMESTAMP
- room_number VARCHAR(50)
- status VARCHAR(20)
- created_at TIMESTAMP
- updated_at TIMESTAMP

FK hospital_id -> hospital(id)
FK patient_id -> patient(id)
INDEX (hospital_id)
```

---

## 7. Billing

```
billing
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- patient_id BIGINT NOT NULL
- admission_id BIGINT
- total_amount DECIMAL(12,2) NOT NULL
- status VARCHAR(20)
- created_at TIMESTAMP
- updated_at TIMESTAMP

FK hospital_id -> hospital(id)
FK patient_id -> patient(id)
FK admission_id -> admission(id)
INDEX (hospital_id)
```

---

## 8. Billing Item

```
billing_item
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- billing_id BIGINT NOT NULL
- item_name VARCHAR(255)
- amount DECIMAL(10,2)
- quantity INT
- created_at TIMESTAMP

FK hospital_id -> hospital(id)
FK billing_id -> billing(id)
INDEX (hospital_id)
```

---

## 9. User

```
user_account
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- username VARCHAR(100) UNIQUE NOT NULL
- password_hash VARCHAR(255) NOT NULL
- role VARCHAR(50)
- is_active BOOLEAN DEFAULT TRUE
- created_at TIMESTAMP

FK hospital_id -> hospital(id)
INDEX (hospital_id)
```

---

## 10. Audit Log

```
audit_log
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- entity_name VARCHAR(100)
- entity_id BIGINT
- action VARCHAR(50)
- performed_by BIGINT
- payload JSON
- created_at TIMESTAMP

INDEX (hospital_id)
```

---

## Constraints Summary

- Every operational table includes `hospital_id`
- All queries must filter by `hospital_id`
- Composite indexes should prioritize (`hospital_id`, other_keys)
- No cross-hospital foreign key relationships

---

## 11. RBAC (Role-Based Access Control)

### Role
```
role
role
- id BIGINT PK
- code VARCHAR(50) UNIQUE NOT NULL          -- SYSTEM_STABLE (e.g., ADMIN, DOCTOR)
- name VARCHAR(100) NOT NULL                -- Display name
- description VARCHAR(255)
- is_system BOOLEAN DEFAULT TRUE            -- Prevent deletion of core roles
- is_active BOOLEAN DEFAULT TRUE
- created_at TIMESTAMP
- updated_at TIMESTAMP


```



### User_Role Mapping
```
user_role
- id BIGINT PK
- user_id BIGINT NOT NULL
- role_id BIGINT NOT NULL
- hospital_id BIGINT NOT NULL              -- 🔥 CRITICAL for your system
- assigned_at TIMESTAMP
- assigned_by BIGINT

FK user_id -> user_account(id)
FK role_id -> role(id)
FK hospital_id -> hospital(id)

UNIQUE (user_id, role_id, hospital_id)
INDEX (hospital_id)
```

---

## 12. Inventory Module

### Item
```
item
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- name VARCHAR(255) NOT NULL
- category VARCHAR(100)
- unit VARCHAR(50)
- created_at TIMESTAMP

FK hospital_id -> hospital(id)
INDEX (hospital_id)
```

### Inventory Stock
```
inventory_stock
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- item_id BIGINT NOT NULL
- quantity DECIMAL(12,2) NOT NULL
- last_updated TIMESTAMP

FK hospital_id -> hospital(id)
FK item_id -> item(id)
UNIQUE (hospital_id, item_id)
INDEX (hospital_id)
```

### Inventory Transaction
```
inventory_transaction
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- item_id BIGINT NOT NULL
- type VARCHAR(20) -- IN / OUT
- quantity DECIMAL(12,2)
- reference VARCHAR(100)
- created_at TIMESTAMP

FK hospital_id -> hospital(id)
FK item_id -> item(id)
INDEX (hospital_id)
```

---

## 13. Pharmacy Module

### Medicine
```
medicine
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- name VARCHAR(255) NOT NULL
- composition VARCHAR(255)
- manufacturer VARCHAR(255)
- created_at TIMESTAMP

FK hospital_id -> hospital(id)
INDEX (hospital_id)
```

### Pharmacy Stock
```
pharmacy_stock
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- medicine_id BIGINT NOT NULL
- batch_number VARCHAR(100)
- expiry_date DATE
- quantity INT
- price DECIMAL(10,2)

FK hospital_id -> hospital(id)
FK medicine_id -> medicine(id)
INDEX (hospital_id)
```

### Prescription
```
prescription
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- patient_id BIGINT NOT NULL
- doctor_hospital_id BIGINT NOT NULL
- created_at TIMESTAMP

FK hospital_id -> hospital(id)
FK patient_id -> patient(id)
FK doctor_hospital_id -> doctor_hospital(id)
INDEX (hospital_id)
```

### Prescription Item
```
prescription_item
- id BIGINT PK
- hospital_id BIGINT NOT NULL
- prescription_id BIGINT NOT NULL
- medicine_id BIGINT NOT NULL
- dosage VARCHAR(100)
- duration VARCHAR(100)

FK hospital_id -> hospital(id)
FK prescription_id -> prescription(id)
FK medicine_id -> medicine(id)
INDEX (hospital_id)
```

---

## Notes

- Global tables: hospital, doctor
- Mapping tables enforce cross-hospital relationships safely
- Backend must inject `hospital_id` in every query

\# HMS API Schema (Refactored - Phase 1)

## Conventions
- Base URL: `/api/v1`
- Authentication: **HTTP Cookie (Session/JWT in cookie)**
- `hospital_id` is NEVER accepted in request body (derived from token/session)
- All responses follow standard format

### Standard Response
```
{
  "success": true,
  "data": {},
  "error": null
}
```

---

## 1. Auth APIs

### Login (Cookie Based)
POST `/api/v1/auth/login`
```
{
  "username": "string",
  "password": "string"
}
```

Response:
- Sets secure HTTP-only cookie
- No token returned in body

### Change Password
POST `/api/v1/auth/change-password`
```
{
  "oldPassword": "string",
  "newPassword": "string"
}
```

### Get Current Profile
GET `/api/v1/auth/me`

### Logout
POST `/api/v1/auth/logout`

---

## 2. Patient APIs

### Create Patient
POST `/patients`
```
{
  "full_name": "string",
  "gender": "string",
  "dob": "YYYY-MM-DD",
  "phone": "string",
  "address": "string"
}
```

### Get Patients
GET `/patients?page=0&size=20`

### Get Patient by ID
GET `/patients/{id}`

---

## 3. Doctor APIs

### Create Doctor
POST `/doctors`
```
{
  "full_name": "string",
  "gender": "string",
  "dob": "YYYY-MM-DD",
  "phone": "string",
  "specialization": "string"
}
```

### Get Doctors
GET `/doctors`

---

## 4. Nurse APIs

### Create Nurse
POST `/nurses`
```
{
  "full_name": "string",
  "gender": "string",
  "dob": "YYYY-MM-DD",
  "phone": "string",
  "department": "string"
}
```

### Get Nurses
GET `/nurses`

---

## 5. User APIs (Role-Based)

### Create User
POST `/users`
```
{
  "username": "string",
  "password": "string",
  "role": "ADMIN | DOCTOR | NURSE | RECEPTIONIST | PHARMACIST",
  "linked_entity_id": 1
}
```

### Get Users
GET `/api/v1/users`

### Reset User Password (Admin Only)
POST `/api/v1/users/{userId}/reset-password`
```
{
  "newPassword": "string"
}
```
*Note: This endpoint allows admins to reset any user's password without knowing the old one.*

---

## 6. Appointment APIs (OPD Focus)

### Create Appointment (Receptionist)
POST `/appointments`
```
{
  "patient_id": 1,
  "doctor_id": 10,
  "appointment_time": "2026-04-20T10:00:00"
}
```

### Update Appointment Status
POST `/appointments/{id}/status`
```
{
  "status": "SCHEDULED | COMPLETED | NO_SHOW | DELAYED | CANCELLED"
}
```

### Get Appointments
GET `/appointments`

---

## 7. Billing APIs (OPD + Pharmacy Only)

### Create OPD Bill
POST `/billings/opd`
```
{
  "patient_id": 1
}
```

### Add Billing Item
POST `/billings/{id}/items`
```
{
  "item_name": "Consultation",
  "amount": 500,
  "quantity": 1
}
```

---

## 8. Inventory APIs

### Add Item
POST `/inventory/items`

### Update Stock
POST `/inventory/stock`
```
{
  "item_id": 1,
  "quantity": 100
}
```

---

## 9. Pharmacy APIs

### Create Prescription
POST `/pharmacy/prescriptions`

### Add Prescription Item
POST `/pharmacy/prescriptions/{id}/items`

### Dispense Medicine (Creates Billing Link)
POST `/pharmacy/dispense`
```
{
  "prescription_id": 1
}
```

---

## 10. Audit APIs

### Get Audit Logs
GET `/audit-logs`

---

## Security Rules

- `hospital_id` must be derived from session
- All queries must be scoped internally
- Unauthorized access must return 403
- Cookie must be HTTP-only and secure

---

## Notes

- No RBAC / Permission APIs
- Role-based access handled in backend logic
- Admission APIs removed (OPD only)
- Billing limited to OPD and Pharmacy
- Payment processing not included
- All APIs must validate input
- Pagination required for list endpoints



## Security Rules

- `hospital_id` must be derived from JWT
- All queries must be scoped internally
- Unauthorized access must return 403

---

## 11. Request/Response DTO Standards

### Principles
- DTOs must be explicit, versioned, and stable
- No entity exposure directly in APIs
- Separate Request and Response DTOs
- No optional ambiguity: use explicit nullability

### Naming
- Request: `CreatePatientRequest`, `UpdatePatientRequest`
- Response: `PatientResponse`

### Example
```
CreatePatientRequest
{
  "full_name": "string",
  "gender": "MALE | FEMALE | OTHER",
  "dob": "YYYY-MM-DD",
  "phone": "string",
  "address": "string"
}

PatientResponse
{
  "id": 1,
  "full_name": "string",
  "gender": "MALE",
  "dob": "YYYY-MM-DD",
  "created_at": "timestamp"
}
```

### Rules
- Never return internal IDs not needed by client
- Always include `id` in response
- Dates must be ISO-8601
- Enums must be controlled values

---

## 12. Error Model (Standardized)

### Structure
```
{
  "success": false,
  "error": {
    "code": "PATIENT_NOT_FOUND",
    "message": "Patient not found",
    "details": {}
  }
}
```

### Categories
- VALIDATION_ERROR
- AUTH_ERROR
- FORBIDDEN
- NOT_FOUND
- BUSINESS_RULE_VIOLATION
- SYSTEM_ERROR

### Rules
- Error codes must be stable and documented
- Messages are for humans, codes are for systems
- Never expose stack traces

---

## 13. Filtering, Search & Pagination

### Standard Query Pattern
GET `/patients?search=ram&status=ACTIVE&page=0&size=20&sort=created_at,desc`

### Supported Parameters
- `search`: generic text search
- `filters`: specific fields (status, date range, etc.)
- `page`, `size`
- `sort=field,asc|desc`

### Example
GET `/appointments?doctor_id=10&from=2026-04-01&to=2026-04-30`

### Rules
- Always apply `hospital_id` filter internally
- Default pagination mandatory
- Max page size must be enforced (e.g., 100)
- Avoid returning large datasets

---

## Notes

- Payment processing is NOT part of the current scope
- Billing module is limited to charge calculation and record management only
- No payment gateway integration, transaction handling, or settlement logic included at this stage
- Future payment integration must be designed as an extension, not embedded into current billing APIs

# HMS License & Subscription Model

## Overview
The HMS platform uses a **license-based subscription model** designed for primarily **on-premise deployments**.

Each deployed instance of HMS is controlled by a **cryptographically signed license**, which determines whether the system is allowed to operate.

This is NOT a SaaS billing system. It is a **runtime enforcement mechanism**.

---

## Core Principle

- One deployment = One hospital = One license
- License determines system access
- System must not function without a valid license

---

## License Structure

The license contains minimal, essential information:

```
{
  "hospital_id": 1,
  "plan": "STANDARD",
  "valid_from": "2026-01-01",
  "valid_till": "2026-12-31"
}
```

This payload is **digitally signed using RSA private key**.

Final license = payload + signature

---

## License Validation

### Validation Rules
- Signature must be valid
- License must not be expired
- `hospital_id` must match system context

### Enforcement
- Validation occurs at backend
- Frontend has no role in enforcement
- System must fail if validation fails

---

## Runtime Behavior

### Request Flow

1. Authenticate request
2. Resolve `hospital_id`
3. Validate license (cached validation allowed)
4. If valid → proceed
5. If invalid → block request

---

## Security Model

- License is signed using **private key (Resilient)**
- HMS backend uses **public key** to verify
- Hospitals cannot generate or modify valid licenses

### Key Rules
- No plain flags like `is_active`
- No trust on external input
- Tampered license must be rejected

---

## Failure Behavior

System must **fail closed**:

- Expired license → deny access
- Invalid signature → deny access
- Missing license → deny access

No partial functionality allowed

---

## Storage & Access

- License can be stored as:
    - File on server OR
    - Database record
- Must be securely read-only from application perspective

---

## Future Extension (Not in Scope Now)

- Feature-based licensing
- User limits
- Module enable/disable
- Online validation / license server

---

## Summary

The HMS license system ensures:

- Controlled deployment usage
- Strong tamper resistance
- Offline operability

It acts as a **secure gatekeeper** for the entire system.