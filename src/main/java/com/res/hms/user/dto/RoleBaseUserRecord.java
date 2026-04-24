package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record RoleBaseUserRecord(
    UUID hospitalId, // Can be null for global roles
    Set<RoleCode> roles, // If empty, removes access for this context
    String specialization,
    String department,
    BigDecimal consultationFee,
    @NotNull(message = "follow Up days required")
        @Min(value = 1, message = "follow Up days must be at least 1")
        Integer followUpDays) {}
