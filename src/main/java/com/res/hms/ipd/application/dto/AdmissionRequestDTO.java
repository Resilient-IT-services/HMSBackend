package com.res.hms.ipd.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class AdmissionRequestDTO {
  @NotNull(message = "Patient ID is required")
  private UUID patientId;

  @NotNull(message = "Doctor ID is required")
  private UUID doctorId;

  @NotNull(message = "Bed ID is required")
  private UUID bedId;

  private String admissionReason;
  private String remarks;
}
