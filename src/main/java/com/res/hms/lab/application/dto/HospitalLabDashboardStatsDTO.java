package com.res.hms.lab.application.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalLabDashboardStatsDTO {
  private long activeLocalTests;
  private long todayPendingOrders;
  private long todayProcessingOrders;
  private long todayVerifiedReports;
  private BigDecimal todayEstimatedRevenue;
}
