package com.res.hms.lab.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderTestResponseDTO {
  private UUID id;
  private UUID testId;
  private String testName;
  private String testCode;
  private UUID packageId;
  private String packageName;
  private String status;
}
