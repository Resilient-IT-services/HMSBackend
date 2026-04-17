/**
 * HMS Backend Working Guide
 *
 * This file is intentionally comment-only.
 * It exists so freshers can open a Java file in the IDE and directly follow referenced classes.
 *
 * This document explains how the current HMS backend is structured at a low level.
 * It is written for developers who are new to Spring Boot or new to this project.
 *
 * The goal is simple:
 * - understand how an HTTP request enters the system
 * - understand which class handles what responsibility
 * - understand how authentication and hospital_id scoping work
 * - understand where to add new code without breaking structure
 *
 * This is not a business requirement document.
 * This is a "how the code works" document.
 *
 * -----------------------------------------------------------------------------
 * 1. Big Picture
 * -----------------------------------------------------------------------------
 *
 * The backend is a layered Spring Boot application.
 *
 * Main flow:
 * 1. Client sends HTTP request
 * 2. Spring Security checks authentication and role access
 * 3. Controller receives request
 * 4. Controller validates request DTO and calls service
 * 5. Service applies business rules
 * 6. Service reads/writes using repository
 * 7. JPA maps Java entities to database tables
 * 8. Controller returns standard API response
 *
 * The code is intentionally split so each layer has one job.
 *
 * -----------------------------------------------------------------------------
 * 2. Project Structure
 * -----------------------------------------------------------------------------
 *
 * Important packages:
 * - com.res.hms.api
 * - com.res.hms.config
 * - com.res.hms.common
 * - com.res.hms.domain
 * - com.res.hms.repository
 * - module packages like patient, doctor, appointment, billing, inventory, pharmacy, audit, user, hospital
 *
 * What each area means:
 * - api: REST controllers. Entry point for HTTP APIs.
 * - config: Spring configuration like security and bootstrap setup.
 * - common: reusable shared code such as API wrappers, exceptions, tenant context.
 * - domain: JPA entities and enums.
 * - repository: Spring Data JPA repositories.
 * - module packages: DTOs, services, and module-specific repositories.
 *
 * -----------------------------------------------------------------------------
 * 3. Layer Responsibility
 * -----------------------------------------------------------------------------
 *
 * 3.1 Controller Layer
 *
 * Controllers are inside com.res.hms.api.
 *
 * Examples:
 * - com.res.hms.api.AuthController
 * - com.res.hms.api.PatientController
 * - com.res.hms.api.AppointmentController
 * - com.res.hms.api.HospitalController
 *
 * Controller responsibilities:
 * - define URL path like /api/v1/patients
 * - define HTTP method like GET, POST
 * - accept request body or query params
 * - trigger bean validation using @Valid
 * - apply role restrictions using @PreAuthorize
 * - call service layer
 * - wrap response using ApiResponse.success(...)
 *
 * Controller should not:
 * - write business rules
 * - access database directly
 * - perform complex logic
 *
 * If controller becomes large, the design is usually wrong.
 *
 * 3.2 Service Layer
 *
 * Services contain business logic.
 *
 * Examples:
 * - com.res.hms.patient.service.PatientService
 * - com.res.hms.billing.service.BillingService
 * - com.res.hms.pharmacy.service.PharmacyService
 * - com.res.hms.hospital.service.HospitalService
 *
 * Service responsibilities:
 * - resolve current hospital_id when operation is hospital-scoped
 * - enforce hospital-scoped access
 * - load dependent records
 * - validate business rules
 * - create/update entities
 * - call repositories
 * - trigger audit logging
 * - convert entities to response DTOs
 *
 * This is the main layer where most new feature work happens.
 *
 * 3.3 Repository Layer
 *
 * Repositories talk to the database.
 *
 * Examples:
 * - com.res.hms.patient.repository.PatientRepository
 * - com.res.hms.doctor.repository.DoctorRepository
 * - com.res.hms.repository.UserAccountRepository
 * - com.res.hms.repository.HospitalRepository
 *
 * Repository responsibilities:
 * - fetch and save JPA entities
 * - expose scoped query methods like findByIdAndHospitalId(...)
 * - avoid business rules
 *
 * Repositories should remain thin.
 *
 * 3.4 Domain Layer
 *
 * Entities in com.res.hms.domain are the Java representation of DB tables.
 *
 * Examples:
 * - com.res.hms.domain.Patient
 * - com.res.hms.domain.UserAccount
 * - com.res.hms.domain.Appointment
 * - com.res.hms.domain.Hospital
 *
 * Entity responsibilities:
 * - define table mapping using JPA annotations
 * - define columns
 * - define relationships like @ManyToOne
 * - store persistent state
 *
 * Entities should not contain API logic.
 *
 * -----------------------------------------------------------------------------
 * 4. Request Flow Example
 * -----------------------------------------------------------------------------
 *
 * Use patient creation as an example.
 *
 * Step 1: HTTP Request
 * POST /api/v1/patients
 * Content-Type: application/json
 * Cookie: JSESSIONID=...
 *
 * Body:
 * {
 *   "full_name": "Asha Nair",
 *   "gender": "FEMALE",
 *   "dob": "1992-08-14",
 *   "phone": "9999999999",
 *   "address": "Kochi"
 * }
 *
 * Step 2: Security
 *
 * Spring Security checks:
 * - is the user authenticated?
 * - does the session cookie map to a valid login?
 * - does the user have one of the roles allowed by controller?
 *
 * For patients, see:
 * - com.res.hms.api.PatientController
 *
 * It uses:
 * @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE')")
 *
 * So only those roles can create a patient.
 *
 * Step 3: Controller
 *
 * The controller:
 * - receives JSON into CreatePatientRequest
 * - validates fields
 * - calls patientService.create(request)
 *
 * Step 4: Service
 *
 * com.res.hms.patient.service.PatientService does the real work:
 * - gets current hospital using HospitalContext
 * - creates Patient entity
 * - sets hospitalId
 * - saves via PatientRepository
 * - writes audit log
 * - returns PatientResponse
 *
 * Step 5: Repository
 *
 * com.res.hms.patient.repository.PatientRepository stores the entity using Spring Data JPA.
 *
 * Step 6: Response
 *
 * Controller wraps result in ApiResponse.
 *
 * -----------------------------------------------------------------------------
 * 5. Authentication Flow
 * -----------------------------------------------------------------------------
 *
 * Authentication is session + HTTP cookie based.
 *
 * Important classes:
 * - com.res.hms.config.SecurityConfig
 * - com.res.hms.api.AuthController
 * - com.res.hms.user.service.AuthService
 * - com.res.hms.user.service.HospitalUserDetailsService
 * - com.res.hms.user.security.HospitalUserPrincipal
 *
 * 5.1 Login
 *
 * Endpoint:
 * - POST /api/v1/auth/login
 *
 * Flow:
 * 1. AuthController.login(...) receives username/password
 * 2. AuthService.login(...) calls Spring AuthenticationManager
 * 3. HospitalUserDetailsService.loadUserByUsername(...) loads user from DB
 * 4. Spring compares raw password with stored bcrypt hash
 * 5. On success, authenticated principal is stored in session
 * 6. Spring returns JSESSIONID cookie
 *
 * 5.2 Cookie
 *
 * Cookie settings are in src/main/resources/application.yml:
 * - http-only: true
 * - same-site: lax
 * - secure configurable via env
 *
 * This means browser JavaScript cannot directly read the cookie.
 *
 * 5.3 Current Logged-in User
 *
 * Endpoint:
 * - GET /api/v1/auth/me
 *
 * This reads the authenticated principal from Spring Security context and returns user info.
 *
 * 5.4 Logout
 *
 * Endpoint:
 * - POST /api/v1/auth/logout
 *
 * This clears authentication/session using SecurityContextLogoutHandler.
 *
 * -----------------------------------------------------------------------------
 * 6. How Role Access Works
 * -----------------------------------------------------------------------------
 *
 * Role restriction happens in two places.
 *
 * 6.1 Authentication Principal
 *
 * The logged-in user becomes a HospitalUserPrincipal.
 *
 * That principal contains:
 * - userId
 * - hospitalId
 * - username
 * - roleCode
 *
 * 6.2 Controller Role Rules
 *
 * Controllers use @PreAuthorize.
 *
 * Examples:
 * - super-admin-only hospital creation
 * - admin-only user creation
 * - receptionist/admin appointment creation
 * - doctor/admin prescription creation
 * - pharmacist/admin dispense access
 *
 * This keeps access rules visible close to each endpoint.
 *
 * -----------------------------------------------------------------------------
 * 7. How hospital_id Works
 * -----------------------------------------------------------------------------
 *
 * This is the most important architectural rule in the project.
 *
 * The system uses one shared database, but each hospital must be isolated logically.
 *
 * Important class:
 * - com.res.hms.common.tenant.HospitalContext
 *
 * 7.1 Where hospital_id comes from
 *
 * It is not taken from request body.
 * It is taken from the logged-in principal stored in the session.
 *
 * That means:
 * - frontend cannot choose hospital freely
 * - backend is the source of truth
 *
 * 7.2 How services use it
 *
 * Most hospital-scoped service methods start with:
 * Long hospitalId = hospitalContext.requireHospitalId();
 *
 * Then repository calls use:
 * - findByIdAndHospitalId(...)
 * - findAllByHospitalId...
 *
 * This prevents cross-hospital access.
 *
 * 7.3 Important split
 *
 * SUPER_ADMIN is global.
 * SUPER_ADMIN has no hospital_id.
 * That role is used for platform-level operations like hospital creation.
 *
 * Normal hospital operations still require hospital-scoped users:
 * - ADMIN
 * - DOCTOR
 * - NURSE
 * - RECEPTIONIST
 * - PHARMACIST
 *
 * HospitalContext.requireHospitalId() rejects SUPER_ADMIN-only access for hospital-scoped operations.
 *
 * 7.4 Rule for future code
 *
 * Whenever you add a new hospital business table:
 * - include hospital_id
 * - always query using hospital_id
 * - never trust client-provided hospital id
 *
 * -----------------------------------------------------------------------------
 * 8. Standard API Response Model
 * -----------------------------------------------------------------------------
 *
 * Important classes:
 * - com.res.hms.common.api.ApiResponse
 * - com.res.hms.common.api.ApiError
 * - com.res.hms.common.api.PageResponse
 *
 * Every controller returns this structure:
 *
 * Success:
 * {
 *   "success": true,
 *   "data": {},
 *   "error": null
 * }
 *
 * Failure:
 * {
 *   "success": false,
 *   "data": null,
 *   "error": {
 *     "code": "NOT_FOUND",
 *     "message": "Patient not found",
 *     "details": {}
 *   }
 * }
 *
 * Why this is useful:
 * - frontend can parse all APIs uniformly
 * - errors stay predictable
 * - pagination stays structured
 *
 * -----------------------------------------------------------------------------
 * 9. Exception Handling
 * -----------------------------------------------------------------------------
 *
 * Important classes:
 * - com.res.hms.common.exception.ApiException
 * - com.res.hms.common.exception.GlobalExceptionHandler
 *
 * How it works:
 * 1. service throws ApiException
 * 2. GlobalExceptionHandler catches it
 * 3. response becomes standardized JSON
 *
 * Example:
 * throw new ApiException("NOT_FOUND", "Patient not found", HttpStatus.NOT_FOUND);
 *
 * Validation errors are also converted into a consistent error shape.
 *
 * This means controllers and services do not need manual try/catch for normal application errors.
 *
 * -----------------------------------------------------------------------------
 * 10. DTOs: Why We Use Them
 * -----------------------------------------------------------------------------
 *
 * DTO means Data Transfer Object.
 *
 * There are two main DTO types:
 * - request DTO
 * - response DTO
 *
 * Examples:
 * - com.res.hms.patient.dto.CreatePatientRequest
 * - com.res.hms.patient.dto.PatientResponse
 *
 * Why DTOs matter:
 * - do not expose entity directly to API
 * - keep API stable even if DB changes
 * - allow validation annotations
 * - control exactly what frontend sees
 *
 * Rule:
 * - API layer uses DTOs
 * - DB layer uses entities
 *
 * Do not return JPA entities directly from controller.
 *
 * -----------------------------------------------------------------------------
 * 11. Database and Flyway
 * -----------------------------------------------------------------------------
 *
 * Important files:
 * - src/main/resources/application.yml
 * - src/main/resources/db/migration/V1__baseline_schema.sql
 * - src/main/resources/db/migration/V2__allow_global_super_admin_users.sql
 *
 * 11.1 Why Flyway is used
 *
 * Flyway version-controls the database schema.
 *
 * This gives:
 * - repeatable setup
 * - controlled schema evolution
 * - easier team collaboration
 *
 * 11.2 Current setup
 *
 * JPA is configured with:
 * ddl-auto: validate
 *
 * This is important.
 *
 * It means:
 * - Hibernate will not create tables
 * - Hibernate will only validate that schema matches entities
 * - actual schema comes from Flyway migration files
 *
 * So if entity changes, migration must also change.
 *
 * 11.3 Rule for future DB changes
 *
 * If you add a new field or table:
 * 1. update entity
 * 2. create new Flyway migration, for example V3__add_visit_table.sql
 * 3. never edit old migration after shared usage
 *
 * -----------------------------------------------------------------------------
 * 12. Bootstrap Data
 * -----------------------------------------------------------------------------
 *
 * Important class:
 * - com.res.hms.config.BootstrapDataConfig
 *
 * On application startup it ensures:
 * - default hospital exists
 * - system roles exist
 * - default super admin user exists
 *
 * This is useful for fresh local setup.
 *
 * Configured from env-backed properties:
 * - HMS_ADMIN_USERNAME
 * - HMS_ADMIN_PASSWORD
 * - HMS_DEFAULT_HOSPITAL_CODE
 * - HMS_DEFAULT_HOSPITAL_NAME
 *
 * If not provided, defaults are used.
 *
 * Important clarification:
 * - bootstrap admin is a SUPER_ADMIN
 * - SUPER_ADMIN is global and has no hospital_id
 * - normal hospital operations still require hospital-scoped users such as ADMIN, DOCTOR, NURSE, RECEPTIONIST, and PHARMACIST
 *
 * This split is intentional:
 * - SUPER_ADMIN manages platform-level setup like hospital creation
 * - hospital-scoped users manage day-to-day operations inside one hospital
 *
 * -----------------------------------------------------------------------------
 * 13. Audit Logging
 * -----------------------------------------------------------------------------
 *
 * Important classes:
 * - com.res.hms.audit.service.AuditLogService
 * - com.res.hms.api.AuditLogController
 * - com.res.hms.domain.AuditLog
 *
 * Audit logging is currently called explicitly from services.
 *
 * Example:
 * - create patient
 * - create doctor
 * - update appointment status
 * - add stock
 * - dispense prescription
 *
 * This is a manual approach for clarity.
 *
 * When adding important business actions, remember to add audit logging in the service.
 *
 * -----------------------------------------------------------------------------
 * 14. How Module Interaction Works
 * -----------------------------------------------------------------------------
 *
 * Modules are not isolated microservices.
 * They are packages inside one monolith.
 *
 * So one service can call another service directly.
 *
 * Examples:
 *
 * 14.1 Appointment Module
 *
 * com.res.hms.appointment.service.AppointmentService interacts with:
 * - PatientService
 * - DoctorService
 * - AppointmentRepository
 * - AuditLogService
 *
 * Why:
 * - appointment must verify patient exists
 * - appointment must verify doctor exists
 * - appointment must be stored
 * - appointment action should be audited
 *
 * 14.2 Pharmacy Module
 *
 * com.res.hms.pharmacy.service.PharmacyService interacts with:
 * - PatientService
 * - DoctorService
 * - MedicineRepository
 * - PharmacyStockRepository
 * - PrescriptionRepository
 * - BillingService
 * - AuditLogService
 *
 * Why:
 * - prescription belongs to patient and doctor
 * - dispensing reduces stock
 * - dispensing generates pharmacy billing
 *
 * 14.3 Billing Module
 *
 * com.res.hms.billing.service.BillingService is used by:
 * - billing controller directly
 * - pharmacy service when medicine is dispensed
 *
 * This is a good example of shared business service reuse.
 *
 * -----------------------------------------------------------------------------
 * 15. Transaction Boundaries
 * -----------------------------------------------------------------------------
 *
 * Many service methods use @Transactional.
 *
 * Meaning:
 * - all DB changes inside that method succeed together
 * - if exception happens, DB changes roll back
 *
 * This is important for operations like:
 * - creating business records
 * - updating stock
 * - dispensing medicines
 * - adding billing items
 *
 * Without transaction handling, partial updates could corrupt data.
 *
 * -----------------------------------------------------------------------------
 * 16. Naming Conventions Used Here
 * -----------------------------------------------------------------------------
 *
 * Java side:
 * - classes: PatientService, BillingResponse
 * - methods: createOpdBilling, updateStatus
 * - fields: fullName, createdAt
 *
 * API side:
 * JSON is exposed as snake_case because of Jackson naming configuration.
 *
 * Example:
 * - Java: fullName
 * - JSON: full_name
 *
 * DB side:
 * Flyway schema uses snake_case.
 *
 * Example:
 * - table: user_account
 * - column: hospital_id
 *
 * -----------------------------------------------------------------------------
 * 17. How To Add a New Module Properly
 * -----------------------------------------------------------------------------
 *
 * Suppose you need to add a new module like Visit.
 *
 * Recommended order:
 * 1. create entity in domain
 * 2. create Flyway migration
 * 3. create repository
 * 4. create request/response DTOs
 * 5. create service
 * 6. create controller
 * 7. add role restrictions
 * 8. add audit logging if action is important
 * 9. add tests
 *
 * Do not start by writing controller only.
 *
 * The controller is the last thin layer, not the core design layer.
 *
 * -----------------------------------------------------------------------------
 * 18. Common Mistakes Freshers Should Avoid
 * -----------------------------------------------------------------------------
 *
 * Mistake 1: Putting business logic in controller
 *
 * Wrong:
 * - validating DB existence in controller
 * - creating entity in controller
 * - writing multiple repository calls in controller
 *
 * Correct:
 * - keep controller thin
 * - move logic to service
 *
 * Mistake 2: Forgetting hospital filter
 *
 * Wrong:
 * - findById(id)
 *
 * Correct:
 * - findByIdAndHospitalId(id, hospitalId)
 *
 * This is a security rule, not a style choice.
 *
 * Mistake 3: Returning entity directly
 *
 * Wrong:
 * - return patient;
 *
 * Correct:
 * - map entity to PatientResponse
 *
 * Mistake 4: Editing DB schema without Flyway migration
 *
 * Wrong:
 * - entity changed, migration not changed
 *
 * Correct:
 * - entity and Flyway migration must stay aligned
 *
 * Mistake 5: Trusting request for hospital_id
 *
 * Wrong:
 * - taking hospital_id from request body
 *
 * Correct:
 * - resolve from authenticated context only
 *
 * Mistake 6: Using SUPER_ADMIN as hospital user
 *
 * Wrong:
 * - using global SUPER_ADMIN for patient, appointment, billing, pharmacy actions
 *
 * Correct:
 * - use hospital-scoped users for hospital operations
 * - use SUPER_ADMIN for platform setup only
 *
 * Mistake 7: Skipping audit on important action
 *
 * For key create/update/dispense operations, add audit logging.
 *
 * -----------------------------------------------------------------------------
 * 19. Current Important Classes To Read First
 * -----------------------------------------------------------------------------
 *
 * Recommended reading order:
 * 1. com.res.hms.config.SecurityConfig
 * 2. com.res.hms.api.AuthController
 * 3. com.res.hms.user.service.AuthService
 * 4. com.res.hms.common.tenant.HospitalContext
 * 5. com.res.hms.common.api.ApiResponse
 * 6. com.res.hms.common.exception.GlobalExceptionHandler
 * 7. com.res.hms.api.HospitalController
 * 8. com.res.hms.api.PatientController
 * 9. com.res.hms.patient.service.PatientService
 * 10. com.res.hms.patient.repository.PatientRepository
 *
 * After that, read:
 * - appointment
 * - billing
 * - pharmacy
 *
 * Those modules show cross-service interaction more clearly.
 *
 * -----------------------------------------------------------------------------
 * 20. Summary
 * -----------------------------------------------------------------------------
 *
 * The project follows a simple rule set:
 * - controllers receive requests
 * - services contain business logic
 * - repositories handle persistence
 * - entities map to tables
 * - DTOs define API contract
 * - security manages login and role access
 * - hospital_id is always backend-enforced
 * - Flyway owns schema changes
 * - audit logs capture important actions
 * - SUPER_ADMIN is global, hospital users are scoped
 *
 * If you keep those boundaries clean, the codebase will remain maintainable.
 * If you blur those boundaries, the codebase will become hard to extend very quickly.
 */
public final class CreateWorkingCode {

    private CreateWorkingCode() {
        // Documentation-only class. Not meant to be instantiated.
    }
}
