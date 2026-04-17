# HMS AI Development Rules

Hospital Management System

This document defines strict development rules for AI coding agents and developers contributing to the HMS backend.

These rules ensure system reliability, patient data safety, and maintainable architecture.

AI tools MUST follow these rules when generating code.

---

# 1. Architecture Rules

HMS uses a strict layered architecture:

controller
service
repository
entity
dto
config
security

Rules:

Controllers
• must NEVER access repositories directly
• must ONLY call services

Services
• contain ALL business logic
• may call multiple repositories

Repositories
• handle ONLY database access

Entities
• represent database tables

DTOs
• define API contracts

---

# 2. Controller Rules

Controllers must be thin.

Controllers must ONLY:

• receive request
• validate request
• call service
• return DTO

Controllers must NEVER:

• contain business logic
• access repositories
• perform calculations

Example:

@RestController
@RequestMapping("/patients")
public class PatientController {

```
private final PatientService patientService;

public PatientController(PatientService patientService) {
    this.patientService = patientService;
}

@GetMapping
public List<PatientDTO> getPatients() {
    return patientService.getAllPatients();
}
```

}

---

# 3. Service Rules

Services contain ALL business logic.

Examples:

• patient lifecycle management
• appointment scheduling
• billing calculation
• insurance processing
• inventory updates

Rules:

• Must use @Service
• Write operations MUST use @Transactional
• May combine multiple repositories
• Must enforce domain rules

---

# 4. Repository Rules

Repositories use Spring Data JPA.

Every repository MUST extend:

JpaRepository<Entity, Long>

Rules:

• NO business logic
• ONLY database operations
• Use custom queries only when necessary

Example:

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

```
List<Appointment> findByPatientId(Long patientId);
```

}

---

# 5. Entity Rules

Entities represent database tables.

Rules:

• Use @Entity
• Primary key must be Long
• Use @GeneratedValue(strategy = GenerationType.IDENTITY)
• No service logic inside entities

Example:

@Entity
public class Patient {

```
@Id  
@GeneratedValue(strategy = GenerationType.IDENTITY)  
private Long id;

private String name;
```

}

---

# 6. Relationship Rules

Use proper JPA relationships.

Examples:

One Patient → Many Appointments

@OneToMany(mappedBy = "patient")
private List<Appointment> appointments;

Appointment → Patient

@ManyToOne
@JoinColumn(name = "patient_id")
private Patient patient;

Rules:

• Avoid unnecessary bidirectional relationships
• Always define ownership clearly

---

# 7. DTO Rules

Entities must NEVER be exposed via APIs.

Use DTOs.

Examples:

PatientDTO
AppointmentDTO
BillingDTO

Rules:

• DTO defines API structure
• Mapping should be explicit
• Avoid exposing internal fields

---

# 8. Transaction Rules

All database write operations MUST be transactional.

Example:

@Transactional
public void createAppointment(AppointmentDTO dto) {
...
}

Read-only operations:

@Transactional(readOnly = true)

---

# 9. Naming Conventions

Classes

PatientService
AppointmentRepository
BillingController

DTOs

PatientDTO
PrescriptionDTO

Entities

Patient
Appointment
Billing

Methods

findByPatientId
findByDoctorId

---

# 10. Medical Data Integrity Rules

HMS handles sensitive healthcare data.

Rules:

• Patient data must NEVER be corrupted
• Use validation for all inputs
• Maintain historical medical records
• NEVER overwrite critical data without audit

---

# 11. Billing & Financial Rules

Financial data must be precise.

Rules:

• ALWAYS use BigDecimal
• NEVER use float or double
• All calculations must be deterministic

Example:

BigDecimal total = price.multiply(quantity);

---

# 12. Appointment Rules

Rules:

• No double booking of doctors
• Validate time slots
• Maintain appointment status

Statuses:

• SCHEDULED
• COMPLETED
• CANCELLED

---

# 13. Prescription Rules

Rules:

• Must be linked to appointment
• Cannot exist without doctor consultation
• Must be immutable after finalization

---

# 14. Inventory Rules

Rules:

• Track stock levels
• Prevent negative stock
• Maintain expiry tracking

---

# 15. Security Rules

Authentication uses JWT.

Rules:

• All endpoints must be secured
• Role-based access control required

Roles:

• ADMIN
• DOCTOR
• NURSE
• STAFF

Security logic must be inside:

security/

NEVER inside services

---

# 16. Logging Rules

Use structured logging.

Example:

log.info("Creating appointment for patient {}", patientId);

Rules:

• Do NOT log sensitive data
• Do NOT log patient medical details

---

# 17. Error Handling Rules

Rules:

• Use global exception handler
• Return proper HTTP status codes

Examples:

400 → Bad Request
401 → Unauthorized
404 → Not Found
500 → Internal Server Error

---

# 18. AI Code Generation Rules

AI tools MUST:

• Follow layered architecture strictly
• Never skip layers
• Never mix service & repository logic
• Use constructor injection
• Avoid circular dependencies
• Prefer clean and readable code

---

# 19. Development Flow

When creating a feature:

1. Create Entity
2. Create Repository
3. Create Service
4. Create Controller
5. Create DTO

NEVER skip any layer

---

# 20. Future Scalability Rules

Architecture must support:

• microservices
• event-driven systems
• real-time monitoring
• AI-based diagnostics

---

# 🚨 FINAL RULE

If any rule conflicts:

👉 Architecture consistency > Speed
👉 Data integrity > Convenience
👉 Security > Everything

---

# AI Reminder

You are building a healthcare system.

Mistakes can impact real patients.

Write code that is:

• safe
• predictable
• maintainable

---
