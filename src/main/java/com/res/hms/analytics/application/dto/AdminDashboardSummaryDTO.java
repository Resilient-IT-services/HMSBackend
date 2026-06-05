package com.res.hms.analytics.application.dto;

import java.math.BigDecimal;
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
public class AdminDashboardSummaryDTO {
  private KpiStats kpis;
  private List<TopHospital> topHospitals;
  private List<RecentRegistration> recentRegistrations;
  private List<SystemAlert> alerts;
  private PlatformHealth health;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class KpiStats {
    private long totalHospitals;
    private long totalUsers;
    private long activeUsersToday;
    private long totalDoctors;
    private BigDecimal totalRevenue;
    private long newHospitalsThisMonth;
    private long totalPatients;
    private String systemUptime;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TopHospital {
    private String name;
    private long patientCount;
    private int performancePercentage;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentRegistration {
    private String fullName;
    private String role;
    private String timeAgo;
    private String initials;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SystemAlert {
    private String message;
    private String timeAgo;
    private String type; // LICENSE, SERVER, STOCK
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlatformHealth {
    private String apiStatus;
    private String dbStatus;
    private String authStatus;
    private String overallUptime;
  }
}
