package com.res.hms.user.dto;

import com.res.hms.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateUserRequest(
    @NotBlank String fullName,
    Gender gender,
    LocalDate dob,
    String phone,
    boolean active,
    String specialization,
    String department) {}
