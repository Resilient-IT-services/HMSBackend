package com.res.hms.clinical.application.prescription.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionLabItemDTO {
  private UUID id;
  private UUID testId;
  private String testName;
  private String notes;
  private String status;
}
