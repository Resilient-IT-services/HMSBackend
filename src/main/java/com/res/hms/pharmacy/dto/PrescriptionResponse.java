package com.res.hms.pharmacy.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
    UUID id,
    UUID patientId,
    String patientName,
    UUID doctorId,
    String doctorName,
    List<PrescriptionItemResponse> items,
    LocalDateTime createdAt) {}
