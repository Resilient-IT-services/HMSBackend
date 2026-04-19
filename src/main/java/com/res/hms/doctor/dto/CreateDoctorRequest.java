package com.res.hms.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDoctorRequest(
    @NotNull(message = "userId is required") Long userId,
    @NotBlank(message = "specialization is required") String specialization) {}
