package com.res.hms.pharmacy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PharmacyStockResponse(
    UUID id,
    UUID medicineId,
    String medicineName,
    String batchNumber,
    LocalDate expiryDate,
    Integer quantity,
    BigDecimal price) {}
