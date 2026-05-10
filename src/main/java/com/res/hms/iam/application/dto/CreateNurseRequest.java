package com.res.hms.iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateNurseRequest(
    @NotNull(message = "userId is required") UUID userId,
    @NotBlank(message = "department is required") String department) {}
