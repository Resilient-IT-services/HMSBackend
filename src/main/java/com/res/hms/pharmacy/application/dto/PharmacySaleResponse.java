package com.res.hms.pharmacy.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PharmacySaleResponse {
  private UUID id;
  private UUID patientId;
  private UUID prescriptionId;
  private BigDecimal totalAmount;
  private List<Item> items;

  @Data
  @Builder
  public static class Item {
    private UUID id;
    private UUID medicineId;
    private String medicineName;
    private UUID stockId;
    private String batchNumber;
    private LocalDate expiryDate;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
  }
}
