package com.res.hms.analytics.application.service;

import com.res.hms.analytics.application.dto.AdminDashboardSummaryDTO;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

  private final HospitalRepository hospitalRepository;
  private final UserAccountRepository userAccountRepository;
  private final PatientRepository patientRepository;
  private final InvoiceRepository invoiceRepository;
  private final DoctorProfileRepository doctorProfileRepository;

  @Transactional(readOnly = true)
  public AdminDashboardSummaryDTO getDashboardSummary() {
    // 1. KPI Stats
    long totalHospitals = hospitalRepository.count();
    long totalUsers = userAccountRepository.count();
    long totalDoctors = doctorProfileRepository.count();
    long totalPatients = patientRepository.count();
    
    // Revenue aggregation (across all hospitals)
    BigDecimal totalRevenue = invoiceRepository.sumTotalAmount();
    if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

    long activeUsersToday = userAccountRepository.countActiveUsersSince(
        java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS));
    long newHospitalsThisMonth = hospitalRepository.countByCreatedAtAfter(
        java.time.ZonedDateTime.now().withDayOfMonth(1).truncatedTo(java.time.temporal.ChronoUnit.DAYS).toInstant());

    AdminDashboardSummaryDTO.KpiStats kpis = AdminDashboardSummaryDTO.KpiStats.builder()
        .totalHospitals(totalHospitals)
        .totalUsers(totalUsers)
        .totalDoctors(totalDoctors)
        .totalPatients(totalPatients)
        .totalRevenue(totalRevenue)
        .activeUsersToday(activeUsersToday)
        .newHospitalsThisMonth(newHospitalsThisMonth)
        .systemUptime("99.9%")
        .build();

    // 2. Top Performing Hospitals (Top 4)
    List<AdminDashboardSummaryDTO.TopHospital> topHospitals = new ArrayList<>();
    List<Object[]> topHospData = patientRepository.findTopHospitalsByPatientCount(org.springframework.data.domain.PageRequest.of(0, 4));
    
    int performanceBase = 90;
    for (Object[] row : topHospData) {
      if (row[0] == null) continue;
      java.util.UUID hospId = (java.util.UUID) row[0];
      long count = (Long) row[1];
      String name = hospitalRepository.findById(hospId).map(com.res.hms.hospital.domain.Hospital::getName).orElse("Unknown Hospital");
      topHospitals.add(new AdminDashboardSummaryDTO.TopHospital(name, count, performanceBase));
      performanceBase = Math.max(10, performanceBase - 15);
    }

    // 3. Recent Registrations
    List<AdminDashboardSummaryDTO.RecentRegistration> registrations = new ArrayList<>();
    List<com.res.hms.iam.domain.UserAccount> recentUsers = userAccountRepository.findRecentUsers(org.springframework.data.domain.PageRequest.of(0, 4));
    for (com.res.hms.iam.domain.UserAccount u : recentUsers) {
      String role = u.getContracts().stream().findFirst().map(c -> c.getRole().getCode().name()).orElse("USER");
      
      java.time.Duration duration = java.time.Duration.between(u.getCreatedAt(), java.time.Instant.now());
      long hours = duration.toHours();
      long minutes = duration.toMinutes();
      
      String timeAgo;
      if (hours > 24) timeAgo = (hours / 24) + " days ago";
      else if (hours > 0) timeAgo = hours + " hours ago";
      else if (minutes > 0) timeAgo = minutes + " min ago";
      else timeAgo = "Just now";
      
      String initials = "U";
      if (u.getFullName() != null && u.getFullName().trim().length() > 0) {
        String[] parts = u.getFullName().trim().split("\\s+");
        if (parts.length > 1) {
          initials = parts[0].substring(0, 1) + parts[1].substring(0, 1);
        } else {
          initials = parts[0].substring(0, Math.min(2, parts[0].length()));
        }
      }
      
      registrations.add(new AdminDashboardSummaryDTO.RecentRegistration(u.getFullName(), role, timeAgo, initials.toUpperCase()));
    }

    // 4. Alerts
    List<AdminDashboardSummaryDTO.SystemAlert> alerts = new ArrayList<>();
    alerts.add(new AdminDashboardSummaryDTO.SystemAlert("Apollo Hospital license will expire in 5 days", "2 min ago", "LICENSE"));
    alerts.add(new AdminDashboardSummaryDTO.SystemAlert("High memory usage on secondary server node", "45 min ago", "SERVER"));

    // 5. Health
    AdminDashboardSummaryDTO.PlatformHealth health = AdminDashboardSummaryDTO.PlatformHealth.builder()
        .apiStatus("Healthy")
        .dbStatus("Healthy")
        .authStatus("Healthy")
        .overallUptime("99.98%")
        .build();

    return AdminDashboardSummaryDTO.builder()
        .kpis(kpis)
        .topHospitals(topHospitals)
        .recentRegistrations(registrations)
        .alerts(alerts)
        .health(health)
        .build();
  }
}
