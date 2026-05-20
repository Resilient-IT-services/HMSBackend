package com.res.hms.pharmacy.application.dto;

import java.math.BigDecimal;
import java.util.List;
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
  private String customerName;
  private String customerPhone;
  private String externalDoctorName;
  private java.time.Instant createdAt;
  private List<Item> items;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private UUID id;
    private String medicineName;
    private String batchNumber;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
  }
}
