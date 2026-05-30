package com.res.hms.tax.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TaxCalculationRequest(
    UUID hospitalId,
    String itemType,
    String itemCode,
    String hsnOrSacCode,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal taxableAmount,
    String billingState,
    String hospitalState,
    Instant invoiceDate,
    String patientCategory,
    String insuranceProvider
) {}
