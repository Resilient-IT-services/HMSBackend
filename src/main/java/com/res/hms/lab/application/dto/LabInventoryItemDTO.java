package com.res.hms.lab.application.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabInventoryItemDTO {
  private UUID id;
  private String itemCode;
  private String itemName;
  private String unit;
  private BigDecimal availableQty;
  private BigDecimal reorderLevel;
}
