package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePrescriptionRequest(
        @NotNull(message = "patient_id is required")
        Long patientId,
        @NotNull(message = "doctor_id is required")
        Long doctorId
) {
}
