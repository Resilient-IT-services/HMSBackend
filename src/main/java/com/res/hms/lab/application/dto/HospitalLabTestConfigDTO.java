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
public class HospitalLabTestConfigDTO {
  private UUID testId;
  private String testCode;
  private String testName;
  private String departmentName;
  private boolean enabled;
  private Integer tatValue;
  private String tatUnit;
  private BigDecimal standardPrice;
  private BigDecimal emergencyPrice;
}
