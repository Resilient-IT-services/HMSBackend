package com.res.hms.clinical.application.prescription.dto;

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
public class PrescriptionDTO {
  private UUID id;
  private UUID patientId;
  private String patientName;
  private UUID doctorId;
  private String doctorName;
  private UUID appointmentId;
  private UUID visitId;
  private java.time.Instant createdAt;
  private List<PrescriptionItemDTO> items;
}
