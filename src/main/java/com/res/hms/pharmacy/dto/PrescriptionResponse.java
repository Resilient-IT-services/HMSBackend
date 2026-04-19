package com.res.hms.pharmacy.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponse(
    Long id,
    Long patientId,
    String patientName,
    Long doctorId,
    String doctorName,
    List<PrescriptionItemResponse> items,
    LocalDateTime createdAt) {}
