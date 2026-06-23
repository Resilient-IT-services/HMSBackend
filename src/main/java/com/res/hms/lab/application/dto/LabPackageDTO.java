package com.res.hms.lab.application.dto;

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
public class LabPackageDTO {
  private UUID id;
  private String packageCode;
  private String packageName;
  private String description;
  private Boolean active;
  private String packageType;
  private Integer versionNo;
  private Boolean isBillable;
  private List<UUID> testIds;
}
