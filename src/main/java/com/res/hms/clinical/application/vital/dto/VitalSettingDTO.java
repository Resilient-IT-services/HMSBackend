package com.res.hms.clinical.application.vital.dto;

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
public class VitalSettingDTO {
  private UUID id;
  private String vitalName;
  private BigDecimal minValue;
  private BigDecimal maxValue;
  private String unit;
}
