package com.res.hms.patient.dto;

import com.res.hms.domain.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long id,
        String fullName,
        Gender gender,
        LocalDate dob,
        String phone,
        String address,
        LocalDateTime createdAt
) {
}
