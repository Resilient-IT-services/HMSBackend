package com.res.hms.doctor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DoctorFeeRequest(
    @NotNull(message = "Doctor is required") Long doctorId,
    @NotNull @DecimalMin(value = "0.01", message = "Fee must be greater than 0")
        BigDecimal consultationFee,
    @NotNull
        @Min(value = 0, message = "Follow-up days must be >= 0")
        @Max(value = 30, message = "Follow-up days max is 30")
        Integer followUpDays) {}
