package com.res.hms.pharmacy.dto;

public record PrescriptionItemResponse(
        Long id,
        Long medicineId,
        String medicineName,
        String dosage,
        String duration
) {
}
