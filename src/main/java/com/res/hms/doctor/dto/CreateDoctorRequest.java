package com.res.hms.doctor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateDoctorRequest(
    @NotNull(message = "userId is required") Long userId,
    @NotBlank(message = "specialization is required") String specialization,
    BigDecimal consultationFee,
    @NotNull(message = "follow Up days required")
        @Min(value = 1, message = "follow Up days must be at least 1")
        Integer followUpDays) {}
