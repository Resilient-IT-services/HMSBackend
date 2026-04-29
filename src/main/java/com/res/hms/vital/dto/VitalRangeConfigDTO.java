package com.res.hms.vital.dto;

import com.res.hms.domain.VitalType;
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
public class VitalRangeConfigDTO {
  private UUID id;
  private VitalType vitalType;
  private BigDecimal minValue;
  private BigDecimal maxValue;
  private String unit;
}
