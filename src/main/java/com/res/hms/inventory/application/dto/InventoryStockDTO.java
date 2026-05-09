package com.res.hms.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockDTO {
  private UUID itemId;
  private String itemName;
  private BigDecimal quantity;
  private LocalDateTime lastUpdated;
}
