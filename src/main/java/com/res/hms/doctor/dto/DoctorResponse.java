package com.res.hms.doctor.dto;

import com.res.hms.domain.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DoctorResponse(
    Long id,
    String fullName,
    Gender gender,
    LocalDate dob,
    String phone,
    String specialization,
    LocalDateTime createdAt) {}
