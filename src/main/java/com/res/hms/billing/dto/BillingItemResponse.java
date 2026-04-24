package com.res.hms.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillingItemResponse(
    UUID id, String itemName, BigDecimal amount, Integer quantity, BigDecimal subtotal) {}
