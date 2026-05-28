package com.res.hms.pharmacy.application.dto;

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
  private String name;
  private String genericName;
  private String composition;
  private String strength;
  private com.res.hms.pharmacy.domain.MedicineForm form;
  private String category;
  private String manufacturer;
  private Integer currentStock;
  private Integer thresholdQuantity;      }

