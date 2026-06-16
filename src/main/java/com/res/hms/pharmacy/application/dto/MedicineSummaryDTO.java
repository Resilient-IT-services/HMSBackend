package com.res.hms.pharmacy.application.dto;

import com.res.hms.pharmacy.domain.DepartmentType;
import com.res.hms.pharmacy.domain.ItemType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineSummaryDTO {
  private UUID id;
  private ItemType itemType;
  private DepartmentType department;
  private String name;
  private String itemCode;
  private String genericName;
  private String composition;
  private String strength;
  private com.res.hms.pharmacy.domain.MedicineForm form;
  private String category;
  private String baseUnit;
  private String manufacturer;
  private String hsnCode;
  private Integer currentStock;
  private Integer thresholdQuantity;
  private Boolean isBatchTracked;
  private Boolean isExpiryTracked;
}
