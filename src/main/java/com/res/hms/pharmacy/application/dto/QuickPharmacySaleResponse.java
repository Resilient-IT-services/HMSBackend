package com.res.hms.pharmacy.application.dto;

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
public class QuickPharmacySaleResponse {
  private UUID saleId;
  private String saleNumber;
  private UUID invoiceId;
  private String invoiceNumber;
  private BigDecimal totalAmount;
  private String status;
  private UUID paymentId;
  private String receiptNumber;
}
