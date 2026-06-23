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
public class LabPackageResultSaveRequest {
  private UUID orderId;
  private UUID packageId;
  private List<PackageTestResultRequest> tests;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PackageTestResultRequest {
    private UUID orderTestId;
    private String findings;
    private List<LabResultSaveRequest.ParameterResultDTO> results;
  }
}
