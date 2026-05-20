package com.res.hms.iam.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
  private long totalUsers;
  private long activeUsers;
  private long totalHospitals;
  private long totalDoctors;
}
