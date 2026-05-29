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
    @jakarta.validation.constraints.Pattern(
        regexp = "^[0-9]{10}$",
        message = "Mobile number must be exactly 10 digits")
    private String customerPhone;
  private String notes;
  @jakarta.validation.Valid private List<Item> items;
  @jakarta.validation.Valid private PaymentInfo payment;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private UUID medicineId;
    private UUID medicineBatchId;
    private Integer quantity;
    private BigDecimal unitPrice;

    @jakarta.validation.constraints.Min(0)
    private BigDecimal discountAmount;

    @jakarta.validation.constraints.Min(0)
    private BigDecimal taxAmount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentInfo {
    private PaymentMode mode;

    @jakarta.validation.constraints.Min(0)
    private BigDecimal amount;

    private String referenceNumber;
  }
}
