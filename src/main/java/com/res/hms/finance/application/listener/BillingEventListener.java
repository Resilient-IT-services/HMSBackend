package com.res.hms.finance.application.listener;

import com.res.hms.clinical.application.event.AppointmentBookedEvent;
import com.res.hms.finance.application.dto.BillingDTO;
import com.res.hms.finance.application.service.BillingService;
import com.res.hms.finance.domain.BillingSourceType;
import com.res.hms.finance.domain.BillingStatus;
import com.res.hms.pharmacy.application.event.PharmacySaleCompletedEvent;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventListener {

  private final BillingService billingService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onAppointmentBooked(AppointmentBookedEvent event) {
    log.info("Generating auto-invoice for appointment: {}", event.appointmentId());

    try {
      BillingDTO billingDto =
          BillingDTO.builder()
              .appointmentId(event.appointmentId())
              .patientId(event.patientId())
              .sourceType(BillingSourceType.OPD)
              .status(BillingStatus.PENDING)
              .totalAmount(BigDecimal.ZERO)
              .items(List.of())
              .build();

      billingService.createBilling(billingDto);
    } catch (Exception e) {
      log.error("Failed to generate auto-invoice for appointment {}", event.appointmentId(), e);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onPharmacySaleCompleted(PharmacySaleCompletedEvent event) {
    log.info("Generating auto-invoice for pharmacy sale: {}", event.saleId());

    try {
      // Note: In a real system, we'd fetch the Sale details to populate the Bill accurately
      BillingDTO billingDto =
          BillingDTO.builder()
              .patientId(event.patientId())
              .sourceType(BillingSourceType.PHARMACY)
              .status(BillingStatus.PENDING)
              .totalAmount(BigDecimal.ZERO)
              .items(List.of())
              .build();

      billingService.createBilling(billingDto);
    } catch (Exception e) {
      log.error("Failed to generate auto-invoice for pharmacy sale {}", event.saleId(), e);
    }
  }
}
