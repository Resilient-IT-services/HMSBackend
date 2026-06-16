package com.res.hms.pharmacy.application.dto;

import com.res.hms.pharmacy.domain.DepartmentType;
import com.res.hms.pharmacy.domain.ItemType;
import jakarta.validation.constraints.NotBlank;
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

  private ItemType itemType;
  private DepartmentType department;

  @NotBlank(message = "Medicine name is required")
  private String name;

  private String itemCode;

  private String genericName;

  private String composition;

  private String strength;

  private com.res.hms.pharmacy.domain.MedicineForm form;

  private com.res.hms.pharmacy.domain.MedicineSchedule scheduleType;
  private String category;
  private String baseUnit;

  private String manufacturer;

  private Integer thresholdQuantity;
  private String hsnCode;

  // Storage Instructions
  private String temperature;
  private String humidity;
  private String specialNotes;

  private Boolean isBatchTracked;
  private Boolean isExpiryTracked;
}
