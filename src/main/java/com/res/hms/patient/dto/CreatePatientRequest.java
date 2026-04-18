package com.res.hms.patient.dto;

import com.res.hms.domain.Gender;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreatePatientRequest(
        @NotBlank(message = "fullName is required")
        String fullName,
        Gender gender,
        LocalDate dob,
        String phone,
        String address
) {
}
