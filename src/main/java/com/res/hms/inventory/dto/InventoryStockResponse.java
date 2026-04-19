package com.res.hms.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryStockResponse(
    Long itemId, String itemName, BigDecimal quantity, LocalDateTime lastUpdated) {}
