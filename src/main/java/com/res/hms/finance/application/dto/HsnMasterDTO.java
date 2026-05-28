package com.res.hms.finance.application.dto;

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
public class HsnMasterDTO {
  private UUID id;
  private String hsnCode;
  private String description;
  private BigDecimal taxPercentage;
  private boolean active;
}
