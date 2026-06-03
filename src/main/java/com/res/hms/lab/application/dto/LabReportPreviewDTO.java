package com.res.hms.lab.application.dto;

import java.time.Instant;
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
public class LabReportPreviewDTO {
  private UUID orderId;
  private String orderNumber;
  private Instant orderedAt;

  // Hospital Info
  private String hospitalName;
  private String hospitalAddress;
  private String hospitalPhone;

  // Patient Info
  private String patientName;
  private String patientGender;
  private String patientAge;
  private String patientId;

  // Doctor Info
  private String doctorName;

  private List<ReportTestGroupDTO> testGroups;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ReportTestGroupDTO {
    private String testName;
    private String testCode;
    private String departmentName;
    private List<ReportParameterDTO> parameters;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ReportParameterDTO {
    private String parameterName;
    private String resultValue;
    private String unit;
    private String referenceRange;
    private Boolean abnormal;
    private Boolean critical;
  }
}
