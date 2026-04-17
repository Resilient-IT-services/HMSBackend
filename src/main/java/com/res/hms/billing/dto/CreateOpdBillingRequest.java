package com.res.hms.billing.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOpdBillingRequest(
        @NotNull(message = "patient_id is required")
        Long patientId
) {
}
