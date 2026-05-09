package com.res.hms.finance.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.infrastructure.repository.BillingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  private final BillingRepository billingRepository;
  private final HospitalContext hospitalContext;

  public List<?> getReportData(
      ReportType type, LocalDate startDate, LocalDate endDate, UUID doctorId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(LocalTime.MAX);

    System.out.println(
        "DEBUG: Report Request - Type: "
            + type
            + ", Start: "
            + start
            + ", End: "
            + end
            + ", Doctor: "
            + doctorId
            + ", Hospital: "
            + hospitalId);

    return switch (type) {
      case DOCTOR_REVENUE ->
          billingRepository.getDoctorRevenueReport(hospitalId, doctorId, start, end);
      case PAYMENT_SUMMARY -> billingRepository.getPaymentSummaryReport(hospitalId, start, end);
      default -> Collections.emptyList();
    };
  }
}
