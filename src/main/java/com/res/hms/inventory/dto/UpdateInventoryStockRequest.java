package com.res.hms.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateInventoryStockRequest(
    @NotNull(message = "itemId is required") UUID itemId,
    @NotNull(message = "quantity is required")
        @DecimalMin(value = "0.01", message = "quantity must be positive")
        BigDecimal quantity) {}
