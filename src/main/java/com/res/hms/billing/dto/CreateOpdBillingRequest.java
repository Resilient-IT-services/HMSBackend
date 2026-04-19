package com.res.hms.billing.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOpdBillingRequest(@NotNull(message = "patientId is required") Long patientId) {}
