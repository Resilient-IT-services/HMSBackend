package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotNull;

public record DispensePrescriptionRequest(
    @NotNull(message = "prescriptionId is required") Long prescriptionId) {}
