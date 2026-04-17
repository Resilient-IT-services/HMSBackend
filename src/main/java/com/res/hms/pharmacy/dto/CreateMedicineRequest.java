package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMedicineRequest(
        @NotBlank(message = "name is required")
        String name,
        String composition,
        String manufacturer
) {
}
