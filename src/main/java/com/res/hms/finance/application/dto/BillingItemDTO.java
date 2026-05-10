package com.res.hms.finance.application.dto;

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
public class BillingItemDTO {
  private UUID id;
  private String itemName;
  private BigDecimal amount;
  private Integer quantity;
  private BigDecimal subtotal;
}
