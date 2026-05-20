package com.res.hms.iam.application.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRevenueSummaryDTO {
  private long totalAppointments;
  private BigDecimal totalRevenue;
  private BigDecimal averageFee;
}
