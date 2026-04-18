package com.res.hms.nurse.dto;

import com.res.hms.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateNurseRequest(
        @NotNull(message = "userId is required")
        Long userId,
        @NotBlank(message = "department is required")
        String department
) {
}
