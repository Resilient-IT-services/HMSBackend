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
  private Boolean active;
  private List<UUID> testIds;
}
