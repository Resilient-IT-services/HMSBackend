package com.res.hms.finance.application.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDTO {
  private String paymentMethod;
  private Long transactionCount;
  private BigDecimal totalAmount;
}
