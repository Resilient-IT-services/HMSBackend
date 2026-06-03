package com.res.hms.lab.application.dto;

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
public class LabReferenceRangeDTO {
  private UUID id;
  private UUID parameterId;
  private String gender; // MALE, FEMALE, ALL
  private Integer minAge;
  private Integer maxAge;
  private String ageUnit; // DAYS, MONTHS, YEARS
  private BigDecimal minValue;
  private BigDecimal maxValue;
  private BigDecimal criticalMin;
  private BigDecimal criticalMax;
}
