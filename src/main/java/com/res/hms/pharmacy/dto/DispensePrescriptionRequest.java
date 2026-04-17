package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotNull;

public record DispensePrescriptionRequest(
        @NotNull(message = "prescription_id is required")
        Long prescriptionId
) {
}
