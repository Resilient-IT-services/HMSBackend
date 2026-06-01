package com.res.hms.pharmacy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @NotBlank(message = "Generic name is required")
  private String genericName;

  @NotBlank(message = "Salt/Composition is required")
  private String composition;

  @NotBlank(message = "Strength is required")
  private String strength;

  @NotNull(message = "Medicine form is required")
  private com.res.hms.pharmacy.domain.MedicineForm form;

  private com.res.hms.pharmacy.domain.MedicineSchedule scheduleType;
  private String category;

  @NotBlank(message = "Manufacturer is required")
  private String manufacturer;

  // Inventory & Batch Information (Initial/Default)
  private String batchNumber;
  private LocalDate expiryDate;
  private Integer currentStock;
  private Integer thresholdQuantity;

  // Pricing & Tax
  private BigDecimal purchasePrice;
  private BigDecimal sellingPrice;
  private String hsnCode;
  private BigDecimal taxPercentage;

  // Storage Instructions
  private String temperature;
  private String humidity;
  private String specialNotes;
}
