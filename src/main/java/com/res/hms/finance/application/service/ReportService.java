package com.res.hms.finance.application.service;

import com.res.hms.core.tenant.HospitalContext;
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
  private final com.res.hms.hospital.infrastructure.repository.HospitalRepository hospitalRepository;
  private final HospitalContext hospitalContext;

  public List<?> getReportData(
      ReportType type, java.time.Instant from, java.time.Instant to, UUID doctorId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    
    if (from == null || to == null) {
        throw new com.res.hms.core.exception.ApiException(
            "VALIDATION_ERROR", "Report range (from and to) is required", org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    return switch (type) {
      case DOCTOR_REVENUE ->
          invoiceItemRepository.getDoctorRevenueReport(hospitalId, doctorId, from, to);
      case PHARMACY_REVENUE ->
          invoiceItemRepository.getPharmacyRevenueReport(hospitalId, from, to);
      case PAYMENT_SUMMARY -> 
          paymentRepository.getPaymentSummaryReport(hospitalId, from, to);
      case PAYMENT_COLLECTOR_SUMMARY -> 
          paymentRepository.getPaymentCollectorReport(hospitalId, from, to);
      default -> Collections.emptyList();
    };
  }
}
