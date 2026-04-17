package com.res.hms.pharmacy.dto;

import java.time.LocalDateTime;

public record MedicineResponse(
        Long id,
        String name,
        String composition,
        String manufacturer,
        LocalDateTime createdAt
) {
}
