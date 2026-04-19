package com.res.hms.pharmacy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PharmacyStockResponse(
    Long id,
    Long medicineId,
    String medicineName,
    String batchNumber,
    LocalDate expiryDate,
    Integer quantity,
    BigDecimal price) {}
