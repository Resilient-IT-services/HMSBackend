package com.res.hms.iam.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSelectionDTO {
  private UUID id;
  private String fullName;
  private String specialization;
  private java.math.BigDecimal consultationFee;
}
