package com.res.hms.tax.application.dto;

import java.math.BigDecimal;

public record TaxComponent(
    String taxCode,
    String taxName,
    BigDecimal taxRate,
    BigDecimal taxAmount
) {}
