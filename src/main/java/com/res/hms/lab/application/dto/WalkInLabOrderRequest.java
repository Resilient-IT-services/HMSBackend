package com.res.hms.lab.application.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkInLabOrderRequest {
  private String walkInName;
  private String walkInPhone;
  private String walkInAddress;
  private List<UUID> testIds;
  private List<UUID> packageIds;
}
