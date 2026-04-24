package com.res.hms.billing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOpdBillingRequest(
    @NotNull(message = "patientId is required") UUID patientId,
    @NotNull(message = "doctorId is required") UUID doctorId) {}
