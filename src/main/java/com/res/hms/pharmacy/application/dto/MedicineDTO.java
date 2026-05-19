package com.res.hms.pharmacy.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineDTO {
  private UUID id;

  @NotBlank(message = "Medicine name is required")
  private String name;

  private String genericName;
  private String composition;
  private String strength;
  private com.res.hms.pharmacy.domain.MedicineForm form;
  private com.res.hms.pharmacy.domain.MedicineSchedule scheduleType;
  private String category;
  private String manufacturer;

  // Inventory & Batch Information (Initial/Default)
  private String batchNumber;
  private LocalDate expiryDate;
  private Integer currentStock;
  private Integer reorderLevel;

  // Pricing & Tax
  private BigDecimal purchasePrice;
  private BigDecimal sellingPrice;
  private BigDecimal taxPercentage;

  // Storage Instructions
  private String temperature;
  private String humidity;
  private String specialNotes;
}
