package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPharmacyStockRequest(
    @NotNull(message = "medicineId is required") Long medicineId,
    @NotBlank(message = "batchNumber is required") String batchNumber,
    LocalDate expiryDate,
    @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,
    @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be positive")
        BigDecimal price) {}
