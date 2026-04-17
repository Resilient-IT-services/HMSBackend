# HMS Backend

Hospital Management System

---

## Overview

HMS (Hospital Management System) is a backend platform designed to manage and streamline hospital operations across patient care, administration, and financial workflows.

The system provides a unified healthcare model capable of handling:

• patient registration
• appointment scheduling
• medical records management
• billing and taxation
• insurance claims
• pharmacy and inventory
• hospital resource management

The backend is built using a modular and scalable architecture suitable for enterprise healthcare systems.

---

## Technology Stack

### Backend

• Java / Spring Boot
• Spring Data JPA
• PostgreSQL / MySQL

### Frontend (Optional / Future)

• React / Next.js

### Infrastructure

• REST APIs
• JWT Authentication
• Docker (future scope)

---

## High Level Architecture

Frontend (Web / Mobile)
│
│ REST API
▼
Spring Boot Backend
│
│ JPA Repositories
▼
Relational Database

External systems (insurance, labs, etc.) may integrate through APIs.

---

## Backend Architecture

The backend follows a clean layered architecture:

controller
service
repository
entity
dto
config
security

---

### Responsibilities of Each Layer

#### Controller

Handles HTTP requests and responses.

Rules

• No business logic
• Calls service layer only
• Returns DTO responses

---

#### Service

Contains all business logic.

Examples

• patient flow management
• billing calculations
• appointment scheduling
• insurance processing

Rules

• No direct controller dependency
• Can use multiple repositories
• All core logic resides here

---

#### Repository

Handles database operations using JPA.

Rules

• No business logic
• Only CRUD operations
• Accessed via services only

---

#### Entity

Represents database tables.

Rules

• Uses JPA annotations
• Contains relationships
• No service logic

---

#### DTO

Used for API request/response.

Rules

• Do not expose entities
• Defines API contract

---

## Project Structure

hms
│
├─ controller
│    ├─ PatientController
│    ├─ AppointmentController
│    ├─ BillingController
│    └─ InventoryController
│
├─ service
│    ├─ PatientService
│    ├─ AppointmentService
│    ├─ BillingService
│    ├─ PharmacyService
│    └─ InventoryService
│
├─ repository
│    ├─ PatientRepository
│    ├─ AppointmentRepository
│    ├─ BillingRepository
│    ├─ PrescriptionRepository
│    ├─ InventoryRepository
│    └─ UserRepository
│
├─ entity
│    ├─ Patient
│    ├─ Appointment
│    ├─ Billing
│    ├─ Prescription
│    ├─ Inventory
│    ├─ User
│    └─ Role
│
├─ dto
├─ config
└─ security

---

## Core Domain Model

The system is based on a patient-centric workflow.

Patient
│
├── Appointment
│
├── Prescription
│
├── Billing
│
└── MedicalRecord

---

## Key Entities

### Patient

Represents a hospital patient.

Fields

• id • name • age • gender • contact • address

---

### Appointment

Represents doctor consultation scheduling.

Fields

• id • patientId • doctorId • date • status

---

### Prescription

Represents medicines prescribed by doctors.

Fields

• id • appointmentId • medicines • notes

---

### Billing

Handles financial transactions.

Fields

• id • patientId • amount • tax • status

---

### Inventory

Tracks hospital stock.

Fields

• id • itemName • quantity • expiryDate

---

### User

Represents system users.

Fields

• id • username • role • password

---

## System Modules

### Patient Management

• Registration
• Appointment
• Queue handling

---

### Treatment Management

• Prescription
• Emergency handling
• Doctor workflows

---

### Financial Management

• Billing
• Taxation
• Insurance claims
• Settlements

---

### Operational Management

• Inventory
• Pharmacy
• Ward & bed management
• Nursing

---

### Analytics

• Reports
• Revenue tracking
• Patient statistics

---

## System Workflow

Patient Journey

Registration
│
▼
Appointment Booking
│
▼
Queue Management
│
▼
Doctor Consultation
│
▼
Prescription
│
▼
Pharmacy / Tests
│
▼
Billing & Payment
│
▼
Medical Record Storage

---

## API Design Principles

The backend exposes REST APIs.

Example endpoints

GET /patients
POST /patients

GET /appointments
POST /appointments

GET /billing
POST /billing

Rules

• APIs return DTOs
• Controllers are thin
• Services contain logic

---

## Security

• JWT Authentication
• Role-Based Access Control (RBAC)
• Secure endpoints

---

## Development Status

Current Phase

Backend architecture and planning

Completed

• module design
• system architecture
• workflow definition

Next Steps

• entity implementation
• service layer
• API development
• authentication

---

## Future Enhancements

• AI-based diagnosis support
• Telemedicine integration
• Mobile application
• Advanced analytics
• Insurance API integration

---

## Conclusion

HMS is designed as a scalable and modular healthcare platform that integrates all hospital operations into a unified system.

It ensures better patient care, operational efficiency, and financial transparency.

---

## Author

Developed as a professional healthcare backend system.

---
