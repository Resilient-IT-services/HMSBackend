package com.res.hms.doctor.dto;

import com.res.hms.domain.Gender;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateDoctorRequest(
        @NotBlank(message = "full_name is required")
        String fullName,
        Gender gender,
        LocalDate dob,
        String phone,
        @NotBlank(message = "specialization is required")
        String specialization
) {
}
