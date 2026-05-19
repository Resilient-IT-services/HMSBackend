package com.res.hms.pharmacy.application.dto;

import com.res.hms.finance.domain.PaymentMode;
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
public class QuickPharmacySaleRequest {
  private com.res.hms.pharmacy.domain.SaleType saleType; // WALKIN, VISIT
  private UUID patientId;
  private UUID visitId;
  private UUID prescriptionId;
  private UUID doctorId;
  private String externalDoctorName;
  private String customerName;
  private String customerPhone;
  private String notes;
  private List<Item> items;
  private PaymentInfo payment;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private UUID medicineId;
    private UUID medicineBatchId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentInfo {
    private PaymentMode mode;
    private BigDecimal amount;
    private String referenceNumber;
  }
}
