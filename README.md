# HMS Backend

Spring Boot backend baseline for the Hospital Management System.

## Modules

- `patient`
- `appointment`
- `billing`
- `pharmacy`
- `inventory`
- `security`

## Current Setup

- Spring Boot `3.4.4`
- Java toolchain `21`
- Spring Web, Validation, JPA, Actuator
- H2 in-memory database for local development

## API

- `GET /api/patients`
- `GET /api/patients/{id}`
- `POST /api/patients`
- `GET /actuator/health`

## Run

Use Gradle from your IDE or add the Gradle wrapper, then run the `HmsBackendApplication` main class.
