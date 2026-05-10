package com.res.hms.iam.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNurseRequest(
    @NotNull(message = "userId is required") UUID userId,
    @NotBlank(message = "department is required") String department) {}
