package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePrescriptionRequest(
        @NotNull(message = "patientId is required")
        Long patientId,
        @NotNull(message = "doctorId is required")
        Long doctorId
) {
}
