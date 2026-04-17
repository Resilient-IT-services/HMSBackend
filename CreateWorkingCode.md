# HMS Backend Working Guide

This document explains how the current HMS backend is structured at a low level.
It is written for developers who are new to Spring Boot or new to this project.

The goal is simple:
- understand how an HTTP request enters the system
- understand which class handles what responsibility
- understand how authentication and `hospital_id` scoping work
- understand where to add new code without breaking structure

This is not a business requirement document.
This is a "how the code works" document.

---

## 1. Big Picture

The backend is a layered Spring Boot application.

Main flow:

1. Client sends HTTP request
2. Spring Security checks authentication and role access
3. Controller receives request
4. Controller validates request DTO and calls service
5. Service applies business rules
6. Service reads/writes using repository
7. JPA maps Java entities to database tables
8. Controller returns standard API response

The code is intentionally split so each layer has one job.

---

## 2. Project Structure

Important folders:

- [src/main/java/com/res/hms/api](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api)
- [src/main/java/com/res/hms/config](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/config)
- [src/main/java/com/res/hms/common](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common)
- [src/main/java/com/res/hms/domain](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/domain)
- [src/main/java/com/res/hms/repository](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/repository)
- module folders like:
  - [patient](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient)
  - [doctor](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/doctor)
  - [appointment](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/appointment)
  - [billing](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/billing)
  - [inventory](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/inventory)
  - [pharmacy](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/pharmacy)
  - [audit](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/audit)
  - [user](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/user)

What each area means:

- `api`: REST controllers. Entry point for HTTP APIs.
- `config`: Spring configuration like security and bootstrap setup.
- `common`: reusable shared code such as API wrappers, exceptions, tenant context.
- `domain`: JPA entities and enums.
- `repository`: Spring Data JPA repositories.
- module folders: DTOs, services, and module-specific repositories.

---

## 3. Layer Responsibility

### 3.1 Controller Layer

Controllers are inside `api`.

Examples:

- [AuthController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/AuthController.java)
- [PatientController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/PatientController.java)
- [AppointmentController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/AppointmentController.java)

Controller responsibilities:

- define URL path like `/api/v1/patients`
- define HTTP method like `GET`, `POST`
- accept request body or query params
- trigger bean validation using `@Valid`
- apply role restrictions using `@PreAuthorize`
- call service layer
- wrap response using `ApiResponse.success(...)`

Controller should not:

- write business rules
- access database directly
- perform complex logic

If controller becomes large, the design is usually wrong.

### 3.2 Service Layer

Services contain business logic.

Examples:

- [PatientService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/service/PatientService.java)
- [BillingService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/billing/service/BillingService.java)
- [PharmacyService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/pharmacy/service/PharmacyService.java)

Service responsibilities:

- resolve current `hospital_id`
- enforce hospital-scoped access
- load dependent records
- validate business rules
- create/update entities
- call repositories
- trigger audit logging
- convert entities to response DTOs

This is the main layer where most new feature work happens.

### 3.3 Repository Layer

Repositories talk to the database.

Examples:

- [PatientRepository.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/repository/PatientRepository.java)
- [DoctorRepository.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/doctor/repository/DoctorRepository.java)
- [UserAccountRepository.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/repository/UserAccountRepository.java)

Repository responsibilities:

- fetch and save JPA entities
- expose scoped query methods like `findByIdAndHospitalId(...)`
- avoid business rules

Repositories should remain thin.

### 3.4 Domain Layer

Entities in `domain` are the Java representation of DB tables.

Examples:

- [Patient.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/domain/Patient.java)
- [UserAccount.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/domain/UserAccount.java)
- [Appointment.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/domain/Appointment.java)

Entity responsibilities:

- define table mapping using JPA annotations
- define columns
- define relationships like `@ManyToOne`
- store persistent state

Entities should not contain API logic.

---

## 4. Request Flow Example

Use patient creation as an example.

### Step 1: HTTP Request

Client sends:

```http
POST /api/v1/patients
Content-Type: application/json
Cookie: JSESSIONID=...
```

Body:

```json
{
  "full_name": "Asha Nair",
  "gender": "FEMALE",
  "dob": "1992-08-14",
  "phone": "9999999999",
  "address": "Kochi"
}
```

### Step 2: Security

Spring Security checks:

- is the user authenticated?
- does the session cookie map to a valid login?
- does the user have one of the roles allowed by controller?

For patients, see [PatientController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/PatientController.java).

It uses:

```java
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE')")
```

So only those roles can create a patient.

### Step 3: Controller

The controller:

- receives JSON into `CreatePatientRequest`
- validates fields
- calls `patientService.create(request)`

### Step 4: Service

[PatientService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/service/PatientService.java) does the real work:

- gets current hospital using `HospitalContext`
- creates `Patient` entity
- sets `hospitalId`
- saves via `PatientRepository`
- writes audit log
- returns `PatientResponse`

### Step 5: Repository

[PatientRepository.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/repository/PatientRepository.java) stores the entity using Spring Data JPA.

### Step 6: Response

Controller wraps result:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "full_name": "Asha Nair",
    "gender": "FEMALE",
    "dob": "1992-08-14",
    "phone": "9999999999",
    "address": "Kochi",
    "created_at": "..."
  },
  "error": null
}
```

---

## 5. Authentication Flow

Authentication is session + HTTP cookie based.

Important files:

- [SecurityConfig.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/config/SecurityConfig.java)
- [AuthController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/AuthController.java)
- [AuthService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/user/service/AuthService.java)
- [HospitalUserDetailsService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/user/service/HospitalUserDetailsService.java)
- [HospitalUserPrincipal.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/user/security/HospitalUserPrincipal.java)

### 5.1 Login

Endpoint:

- `POST /api/v1/auth/login`

Flow:

1. `AuthController.login(...)` receives username/password
2. `AuthService.login(...)` calls Spring `AuthenticationManager`
3. `HospitalUserDetailsService.loadUserByUsername(...)` loads user from DB
4. Spring compares raw password with stored bcrypt hash
5. On success, authenticated principal is stored in session
6. Spring returns `JSESSIONID` cookie

### 5.2 Cookie

Cookie settings are in [application.yml](/abs/path/C:/Code/HMSBackend/src/main/resources/application.yml):

- `http-only: true`
- `same-site: lax`
- `secure` configurable via env

This means browser JavaScript cannot directly read the cookie.

### 5.3 Current Logged-in User

Endpoint:

- `GET /api/v1/auth/me`

This reads the authenticated principal from Spring Security context and returns user info.

### 5.4 Logout

Endpoint:

- `POST /api/v1/auth/logout`

This clears authentication/session using `SecurityContextLogoutHandler`.

---

## 6. How Role Access Works

Role restriction happens in two places.

### 6.1 Authentication Principal

The logged-in user becomes a `HospitalUserPrincipal`.

That principal contains:

- `userId`
- `hospitalId`
- `username`
- `roleCode`

### 6.2 Controller Role Rules

Controllers use `@PreAuthorize`.

Examples:

- admin-only user creation
- receptionist/admin appointment creation
- doctor/admin prescription creation
- pharmacist/admin dispense access

This keeps access rules visible close to each endpoint.

---

## 7. How `hospital_id` Works

This is the most important architectural rule in the project.

The system uses one shared database, but each hospital must be isolated logically.

Important file:

- [HospitalContext.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/tenant/HospitalContext.java)

### 7.1 Where hospital_id comes from

It is not taken from request body.

It is taken from the logged-in principal stored in the session.

That means:

- frontend cannot choose hospital freely
- backend is the source of truth

### 7.2 How services use it

Most service methods start with:

```java
Long hospitalId = hospitalContext.requireHospitalId();
```

Then repository calls use:

- `findByIdAndHospitalId(...)`
- `findAllByHospitalId...`

This prevents cross-hospital access.

### 7.3 Rule for future code

Whenever you add a new business table:

- include `hospital_id`
- always query using `hospital_id`
- never trust client-provided hospital id

---

## 8. Standard API Response Model

Important files:

- [ApiResponse.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/api/ApiResponse.java)
- [ApiError.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/api/ApiError.java)
- [PageResponse.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/api/PageResponse.java)

Every controller returns this structure:

Success:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

Failure:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOT_FOUND",
    "message": "Patient not found",
    "details": {}
  }
}
```

Why this is useful:

- frontend can parse all APIs uniformly
- errors stay predictable
- pagination stays structured

---

## 9. Exception Handling

Important files:

- [ApiException.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/exception/ApiException.java)
- [GlobalExceptionHandler.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/exception/GlobalExceptionHandler.java)

How it works:

1. service throws `ApiException`
2. `GlobalExceptionHandler` catches it
3. response becomes standardized JSON

Example:

```java
throw new ApiException("NOT_FOUND", "Patient not found", HttpStatus.NOT_FOUND);
```

Validation errors are also converted into a consistent error shape.

This means controllers and services do not need manual `try/catch` for normal application errors.

---

## 10. DTOs: Why We Use Them

DTO means Data Transfer Object.

There are two main DTO types:

- request DTO
- response DTO

Examples:

- [CreatePatientRequest.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/dto/CreatePatientRequest.java)
- [PatientResponse.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/dto/PatientResponse.java)

Why DTOs matter:

- do not expose entity directly to API
- keep API stable even if DB changes
- allow validation annotations
- control exactly what frontend sees

Rule:

- API layer uses DTOs
- DB layer uses entities

Do not return JPA entities directly from controller.

---

## 11. Database and Flyway

Important files:

- [application.yml](/abs/path/C:/Code/HMSBackend/src/main/resources/application.yml)
- [V1__baseline_schema.sql](/abs/path/C:/Code/HMSBackend/src/main/resources/db/migration/V1__baseline_schema.sql)

### 11.1 Why Flyway is used

Flyway version-controls the database schema.

This gives:

- repeatable setup
- controlled schema evolution
- easier team collaboration

### 11.2 Current setup

JPA is configured with:

```yaml
ddl-auto: validate
```

This is important.

It means:

- Hibernate will not create tables
- Hibernate will only validate that schema matches entities
- actual schema comes from Flyway migration files

So if entity changes, migration must also change.

### 11.3 Rule for future DB changes

If you add a new field or table:

1. update entity
2. create new Flyway migration, for example `V2__add_visit_table.sql`
3. never edit old migration after shared usage

---

## 12. Bootstrap Data

Important file:

- [BootstrapDataConfig.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/config/BootstrapDataConfig.java)

On application startup it ensures:

- default hospital exists
- system roles exist
- default admin user exists

This is useful for fresh local setup.

Configured from env-backed properties:

- `HMS_ADMIN_USERNAME`
- `HMS_ADMIN_PASSWORD`
- `HMS_DEFAULT_HOSPITAL_CODE`
- `HMS_DEFAULT_HOSPITAL_NAME`

If not provided, defaults are used.

---

## 13. Audit Logging

Important files:

- [AuditLogService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/audit/service/AuditLogService.java)
- [AuditLogController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/AuditLogController.java)
- [AuditLog.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/domain/AuditLog.java)

Audit logging is currently called explicitly from services.

Example:

- create patient
- create doctor
- update appointment status
- add stock
- dispense prescription

This is a manual approach for clarity.

When adding important business actions, remember to add audit logging in the service.

---

## 14. How Module Interaction Works

Modules are not isolated microservices.
They are packages inside one monolith.

So one service can call another service directly.

Examples:

### 14.1 Appointment Module

[AppointmentService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/appointment/service/AppointmentService.java) interacts with:

- `PatientService`
- `DoctorService`
- `AppointmentRepository`
- `AuditLogService`

Why:

- appointment must verify patient exists
- appointment must verify doctor exists
- appointment must be stored
- appointment action should be audited

### 14.2 Pharmacy Module

[PharmacyService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/pharmacy/service/PharmacyService.java) interacts with:

- `PatientService`
- `DoctorService`
- `MedicineRepository`
- `PharmacyStockRepository`
- `PrescriptionRepository`
- `BillingService`
- `AuditLogService`

Why:

- prescription belongs to patient and doctor
- dispensing reduces stock
- dispensing generates pharmacy billing

### 14.3 Billing Module

[BillingService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/billing/service/BillingService.java) is used by:

- billing controller directly
- pharmacy service when medicine is dispensed

This is a good example of shared business service reuse.

---

## 15. Transaction Boundaries

Many service methods use `@Transactional`.

Meaning:

- all DB changes inside that method succeed together
- if exception happens, DB changes roll back

This is important for operations like:

- creating business records
- updating stock
- dispensing medicines
- adding billing items

Without transaction handling, partial updates could corrupt data.

---

## 16. Naming Conventions Used Here

### Java side

- classes: `PatientService`, `BillingResponse`
- methods: `createOpdBilling`, `updateStatus`
- fields: `fullName`, `createdAt`

### API side

JSON is exposed as `snake_case` because of Jackson naming configuration.

Example:

- Java: `fullName`
- JSON: `full_name`

### DB side

Flyway schema uses `snake_case`.

Example:

- table: `user_account`
- column: `hospital_id`

---

## 17. How To Add a New Module Properly

Suppose you need to add a new module like `Visit`.

Recommended order:

1. create entity in `domain`
2. create Flyway migration
3. create repository
4. create request/response DTOs
5. create service
6. create controller
7. add role restrictions
8. add audit logging if action is important
9. add tests

Do not start by writing controller only.

The controller is the last thin layer, not the core design layer.

---

## 18. Common Mistakes Freshers Should Avoid

### Mistake 1: Putting business logic in controller

Wrong:

- validating DB existence in controller
- creating entity in controller
- writing multiple repository calls in controller

Correct:

- keep controller thin
- move logic to service

### Mistake 2: Forgetting hospital filter

Wrong:

- `findById(id)`

Correct:

- `findByIdAndHospitalId(id, hospitalId)`

This is a security rule, not a style choice.

### Mistake 3: Returning entity directly

Wrong:

- `return patient;`

Correct:

- map entity to `PatientResponse`

### Mistake 4: Editing DB schema without Flyway migration

Wrong:

- entity changed, migration not changed

Correct:

- entity and Flyway migration must stay aligned

### Mistake 5: Trusting request for hospital_id

Wrong:

- taking `hospital_id` from request body

Correct:

- resolve from authenticated context only

### Mistake 6: Skipping audit on important action

For key create/update/dispense operations, add audit logging.

---

## 19. Current Important Files To Read First

If someone is new to the codebase, this is the recommended reading order:

1. [application.yml](/abs/path/C:/Code/HMSBackend/src/main/resources/application.yml)
2. [SecurityConfig.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/config/SecurityConfig.java)
3. [AuthController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/AuthController.java)
4. [AuthService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/user/service/AuthService.java)
5. [HospitalContext.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/tenant/HospitalContext.java)
6. [ApiResponse.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/api/ApiResponse.java)
7. [GlobalExceptionHandler.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/common/exception/GlobalExceptionHandler.java)
8. [PatientController.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/api/PatientController.java)
9. [PatientService.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/service/PatientService.java)
10. [PatientRepository.java](/abs/path/C:/Code/HMSBackend/src/main/java/com/res/hms/patient/repository/PatientRepository.java)
11. [V1__baseline_schema.sql](/abs/path/C:/Code/HMSBackend/src/main/resources/db/migration/V1__baseline_schema.sql)

After that, read:

- appointment
- billing
- pharmacy

Those modules show cross-service interaction more clearly.

---

## 20. Summary

The project follows a simple rule set:

- controllers receive requests
- services contain business logic
- repositories handle persistence
- entities map to tables
- DTOs define API contract
- security manages login and role access
- `hospital_id` is always backend-enforced
- Flyway owns schema changes
- audit logs capture important actions

If you keep those boundaries clean, the codebase will remain maintainable.

If you blur those boundaries, the codebase will become hard to extend very quickly.

