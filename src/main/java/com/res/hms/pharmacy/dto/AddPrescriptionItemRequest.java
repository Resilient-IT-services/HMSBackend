package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddPrescriptionItemRequest(
        @NotNull(message = "medicineId is required")
        Long medicineId,
        @NotBlank(message = "dosage is required")
        String dosage,
        @NotBlank(message = "duration is required")
        String duration
) {
}
