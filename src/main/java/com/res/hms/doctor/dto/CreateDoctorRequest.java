package com.res.hms.doctor.dto;

import com.res.hms.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDoctorRequest(
        @NotNull(message = "userId is required")
        Long userId,
        @NotBlank(message = "specialization is required")
        String specialization
) {
}
