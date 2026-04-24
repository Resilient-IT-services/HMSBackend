package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DispensePrescriptionRequest(
    @NotNull(message = "prescriptionId is required") UUID prescriptionId) {}
