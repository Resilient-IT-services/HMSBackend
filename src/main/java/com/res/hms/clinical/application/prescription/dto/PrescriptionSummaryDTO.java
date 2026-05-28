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
public class PrescriptionSummaryDTO {
  private UUID id;
  private UUID patientId;
  private String patientName;
  private String patientCode;
  private UUID doctorId;
  private String doctorName;
  private UUID appointmentId;
  private UUID visitId;
  private String status;
  private java.time.Instant createdAt;
  private Integer itemCount;
}
