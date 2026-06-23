package com.res.hms.lab.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalLabPackageConfigDTO {
  private UUID packageId;
  private String packageCode;
  private String packageName;
  private String description;
  private String packageType;
  private Integer versionNo;
  private boolean enabled;
  private BigDecimal standardPrice;
  private BigDecimal emergencyPrice;
  private List<UUID> testIds;
}
