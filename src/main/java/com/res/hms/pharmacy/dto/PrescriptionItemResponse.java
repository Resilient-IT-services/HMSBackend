package com.res.hms.pharmacy.dto;

import java.util.UUID;

public record PrescriptionItemResponse(
    UUID id, UUID medicineId, String medicineName, String dosage, String duration) {}
