package com.res.hms.billing.dto;

import java.math.BigDecimal;

public record BillingItemResponse(
        Long id,
        String itemName,
        BigDecimal amount,
        Integer quantity,
        BigDecimal subtotal
) {
}
