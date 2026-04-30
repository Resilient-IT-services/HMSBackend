package com.res.hms.inventory.dto;

import com.res.hms.domain.InventoryTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class StockTransactionRequest {
  @NotNull(message = "Item ID is required")
  private UUID itemId;

  @NotNull(message = "Transaction type is required")
  private InventoryTransactionType type;

  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be positive")
  private BigDecimal quantity;

  private String reference;
}
