package com.res.hms.finance.application.service;

import com.res.hms.core.tenant.HospitalContext;
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

  private final com.res.hms.finance.infrastructure.repository.InvoiceItemRepository invoiceItemRepository;
  private final com.res.hms.finance.infrastructure.repository.PaymentRepository paymentRepository;
  private final HospitalContext hospitalContext;

  public List<?> getReportData(
      ReportType type, LocalDate startDate, LocalDate endDate, UUID doctorId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(LocalTime.MAX);

    return switch (type) {
      case DOCTOR_REVENUE ->
          invoiceItemRepository.getDoctorRevenueReport(hospitalId, doctorId, start, end);
      case PHARMACY_REVENUE ->
          invoiceItemRepository.getPharmacyRevenueReport(hospitalId, start, end);
      case PAYMENT_SUMMARY -> 
          paymentRepository.getPaymentSummaryReport(hospitalId, start, end);
      case PAYMENT_COLLECTOR_SUMMARY -> 
          paymentRepository.getPaymentCollectorReport(hospitalId, start, end);
      default -> Collections.emptyList();
    };
  }
}
