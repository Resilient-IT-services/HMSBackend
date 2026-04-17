package com.res.hms.nurse.dto;

import com.res.hms.domain.Gender;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateNurseRequest(
        @NotBlank(message = "full_name is required")
        String fullName,
        Gender gender,
        LocalDate dob,
        String phone,
        @NotBlank(message = "department is required")
        String department
) {
}
