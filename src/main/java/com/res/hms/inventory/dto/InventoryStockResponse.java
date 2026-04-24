package com.res.hms.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryStockResponse(
    UUID itemId, String itemName, BigDecimal quantity, LocalDateTime lastUpdated) {}
