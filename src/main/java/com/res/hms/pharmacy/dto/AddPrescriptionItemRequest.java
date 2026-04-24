package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddPrescriptionItemRequest(
    @NotNull(message = "medicineId is required") UUID medicineId,
    @NotBlank(message = "dosage is required") String dosage,
    @NotBlank(message = "duration is required") String duration) {}
