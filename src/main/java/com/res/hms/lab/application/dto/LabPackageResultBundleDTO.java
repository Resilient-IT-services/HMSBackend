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
public class LabPackageResultBundleDTO {
  private UUID orderId;
  private UUID packageId;
  private String packageName;
  private String status;
  private List<PackageTestBundleDTO> tests;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PackageTestBundleDTO {
    private UUID orderTestId;
    private UUID testId;
    private String testName;
    private String category;
    private String resultEntryType;
    private String status;
    private String findings;
    private List<LabTestParameterDTO> parameters;
  }
}
