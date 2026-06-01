package com.res.hms.tax.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record TaxCalculationResult(
    BigDecimal taxableAmount,
    BigDecimal totalTax,
    BigDecimal grandTotal,
    List<TaxComponent> components
) {}
