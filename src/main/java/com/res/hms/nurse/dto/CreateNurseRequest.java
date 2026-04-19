package com.res.hms.nurse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNurseRequest(
    @NotNull(message = "userId is required") Long userId,
    @NotBlank(message = "department is required") String department) {}
