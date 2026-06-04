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
public class LabOrderResponseDTO {
  private UUID id;
  private UUID patientId;
  private String patientName;
  private String orderNumber;
  private String status;
  private Instant orderedAt;
  private List<LabOrderTestResponseDTO> tests;
}
