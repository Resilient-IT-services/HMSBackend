package com.res.hms.patient.dto;

import com.res.hms.domain.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponse(
    UUID id,
    String fullName,
    String patientCode,
    String gender,
    LocalDate dob,
    String phone,
    String address,
    LocalDateTime createdAt) {}
