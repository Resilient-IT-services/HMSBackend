package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePrescriptionRequest(
    @NotNull(message = "patientId is required") UUID patientId,
    @NotNull(message = "doctorId is required") UUID doctorId) {}
