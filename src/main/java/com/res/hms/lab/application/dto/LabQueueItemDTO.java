package com.res.hms.lab.application.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabQueueItemDTO {
  private UUID orderId;
  private UUID orderTestId;
  private String orderNumber;
  private UUID patientId;
  private String patientName;
  private String doctorName;
  private String testName;
  private String status;
  private Instant orderedAt;
}
