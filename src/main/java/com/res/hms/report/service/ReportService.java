package com.res.hms.report.service;

import com.res.hms.billing.repository.BillingRepository;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.report.dto.PaymentSummaryDTO;
import com.res.hms.report.dto.ReportType;
import com.res.hms.report.dto.RevenueReportDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  private final BillingRepository billingRepository;
  private final HospitalContext hospitalContext;

  public List<?> getReportData(ReportType type, LocalDate startDate, LocalDate endDate, UUID doctorId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(LocalTime.MAX);

    System.out.println("DEBUG: Report Request - Type: " + type + ", Start: " + start + ", End: " + end + ", Doctor: " + doctorId + ", Hospital: " + hospitalId);

    return switch (type) {
      case DOCTOR_REVENUE -> billingRepository.getDoctorRevenueReport(hospitalId, doctorId, start, end);
      case PAYMENT_SUMMARY -> billingRepository.getPaymentSummaryReport(hospitalId, start, end);
      default -> Collections.emptyList();
    };
  }
}
