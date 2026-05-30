package com.res.hms.finance.application.listener;

import com.res.hms.clinical.application.event.AppointmentBookedEvent;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentCharge;
import com.res.hms.clinical.domain.AppointmentChargeItem;
import com.res.hms.clinical.infrastructure.repository.AppointmentChargeRepository;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.pharmacy.application.event.PharmacySaleCompletedEvent;
import com.res.hms.pharmacy.domain.PharmacySale;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.res.hms.tax.application.dto.TaxCalculationRequest;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.service.TaxEngineService;
import com.res.hms.clinical.domain.AppointmentChargeTax;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialIntegrationListener {

  private final com.res.hms.finance.application.service.InvoiceService invoiceService;
  private final com.res.hms.clinical.infrastructure.repository.AppointmentRepository
      appointmentRepository;
  private final com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository
      doctorHospitalDetailsRepository;
  private final com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository
      pharmacySaleRepository;
  private final com.res.hms.finance.infrastructure.repository.InvoiceItemRepository
      invoiceItemRepository;
  private final AppointmentChargeRepository appointmentChargeRepository;
  private final TaxEngineService taxEngineService;

  @org.springframework.transaction.event.TransactionalEventListener(
      phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void onAppointmentBooked(AppointmentBookedEvent event) {
    log.info("RECEIVED AppointmentBookedEvent for appointment: {}", event.appointmentId());

    try {
      // 1. Deduplication: Check if charge already exists
      if (appointmentChargeRepository
          .findByAppointmentIdAndHospitalId(event.appointmentId(), event.hospitalId())
          .isPresent()) {
        log.info(
            "DEDUPLICATION: Charge already exists for appointment {}, skipping.",
            event.appointmentId());
        return;
      }

      // Legacy deduplication check
      if (invoiceItemRepository.existsBySourceTypeAndSourceId(
          FinanceSourceType.APPOINTMENT, event.appointmentId())) {
        log.info(
            "DEDUPLICATION: Invoice already exists for appointment {}, skipping auto-generation.",
            event.appointmentId());
        return;
      }

      log.info("NO DEDUPLICATION: Generating charge for appointment {}...", event.appointmentId());

      Appointment appointment =
          appointmentRepository
              .findById(event.appointmentId())
              .orElseThrow(
                  () -> new RuntimeException("Appointment not found: " + event.appointmentId()));

      // 2. Get Consultation Fee
      BigDecimal fee =
          doctorHospitalDetailsRepository
              .findByUserIdAndHospitalId(appointment.getDoctorId(), event.hospitalId())
              .map(DoctorHospitalDetails::getConsultationFee)
              .orElse(BigDecimal.ZERO);

      // 3. Tax Calculation
      TaxCalculationRequest taxRequest = TaxCalculationRequest.builder()
          .hospitalId(event.hospitalId())
          .itemType("CONSULTATION")
          .itemCode("CONS-" + appointment.getDoctorId())
          .taxableAmount(fee)
          .quantity(BigDecimal.ONE)
          .unitPrice(fee)
          .invoiceDate(Instant.now())
          .build();
          
      TaxCalculationResult taxResult = taxEngineService.calculate(taxRequest);

      // 4. Create AppointmentCharge (Business side)
      AppointmentCharge charge = new AppointmentCharge();
      charge.setHospitalId(event.hospitalId());
      charge.setPatientId(event.patientId());
      charge.setAppointmentId(event.appointmentId());
      charge.setDoctorId(appointment.getDoctorId());
      charge.setDoctorName(appointment.getDoctorName());
      charge.setSourceType("CONSULTATION");
      charge.setStatus("COMPLETED");
      charge.setTaxableAmount(taxResult.taxableAmount());
      charge.setTaxAmount(taxResult.totalTax());
      charge.setTotalAmount(taxResult.grandTotal());

      AppointmentChargeItem chargeItem = new AppointmentChargeItem();
      chargeItem.setHospitalId(event.hospitalId());
      chargeItem.setCharge(charge);
      chargeItem.setItemName("Consultation - " + appointment.getDoctorName());
      chargeItem.setAmount(fee);
      chargeItem.setQuantity(1);
      chargeItem.setSubtotal(fee);

      charge.getItems().add(chargeItem);

      // Save snapshots
      if (taxResult.components() != null) {
        taxResult.components().forEach(c -> {
          AppointmentChargeTax act = new AppointmentChargeTax();
          act.setCharge(charge);
          act.setTaxCode(c.taxCode());
          act.setTaxName(c.taxName());
          act.setTaxRate(c.taxRate());
          act.setTaxAmount(c.taxAmount());
          act.setTaxableAmount(taxResult.taxableAmount());
          charge.getTaxes().add(act);
        });
      }

      // 5. Get or Create Invoice (Finance side)
      Invoice invoice =
          invoiceService.getOrCreateInvoice(
              event.hospitalId(), event.patientId(), appointment.getVisitId());

      // 6. Add Consultation Item to Invoice with Tax Snapshot
      invoiceService.addItem(
          invoice.getId(),
          chargeItem.getItemName(),
          InvoiceItemCategory.CONSULTATION,
          BigDecimal.ONE,
          fee,
          BigDecimal.ZERO, // discount
          FinanceSourceType.APPOINTMENT,
          event.appointmentId(),
          appointment.getDoctorId(),
          appointment.getDoctorName(),
          taxResult.components());

      // 7. Link charge to invoice
      charge.setInvoiceId(invoice.getId());
      appointmentChargeRepository.save(charge);

      log.info(
          "Evolved Finance: Charge and Invoice {} created for appointment {}",
          invoice.getInvoiceNumber(),
          event.appointmentId());
    } catch (Exception e) {
      log.error(
          "Evolved Finance: Failed to generate charge/invoice for appointment {}",
          event.appointmentId(),
          e);
    }
  }

  @EventListener
  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
  public void onPharmacySaleCompleted(PharmacySaleCompletedEvent event) {
    log.info("Evolved Finance: Generating invoice for pharmacy sale: {}", event.saleId());

    try {
      PharmacySale sale =
          pharmacySaleRepository
              .findById(event.saleId())
              .orElseThrow(() -> new RuntimeException("Pharmacy sale not found"));

      // 1. Get or Create Invoice
      Invoice invoice =
          invoiceService.getOrCreateInvoice(
              event.hospitalId(), event.patientId(), sale.getVisitId());

      // 2. Add Pharmacy Items
      sale.getItems()
          .forEach(
              item -> {
                invoiceService.addItem(
                    invoice.getId(),
                    "Medicine - " + item.getMedicine().getName(),
                    InvoiceItemCategory.PHARMACY,
                    BigDecimal.valueOf(item.getQuantity()),
                    item.getUnitPrice(),
                    item.getDiscountAmount(),
                    FinanceSourceType.PHARMACY_SALE_ITEM,
                    item.getId(),
                    null,
                    null,
                    null); // Skipping pharmacy taxes for now to focus on Appointment
              });

      log.info(
          "Evolved Finance: Invoice {} created for pharmacy sale {}",
          invoice.getInvoiceNumber(),
          event.saleId());
    } catch (Exception e) {
      log.error(
          "Evolved Finance: Failed to generate invoice for pharmacy sale {}", event.saleId(), e);
    }
  }
}

