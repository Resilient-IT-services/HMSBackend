package com.res.hms.finance.application.listener;

import com.res.hms.clinical.application.event.AppointmentBookedEvent;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository;
import com.res.hms.pharmacy.application.event.PharmacySaleCompletedEvent;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialIntegrationListener {

    private final com.res.hms.finance.application.service.InvoiceService invoiceService;
    private final com.res.hms.clinical.infrastructure.repository.AppointmentRepository appointmentRepository;
    private final com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
    private final com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository pharmacySaleRepository;
    private final com.res.hms.finance.infrastructure.repository.InvoiceItemRepository invoiceItemRepository;

    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onAppointmentBooked(AppointmentBookedEvent event) {
        log.info("RECEIVED AppointmentBookedEvent for appointment: {}", event.appointmentId());

        try {
            // Deduplication: If an invoice item already exists for this appointment, skip.
            // This happens when the OPDOrchestrator already created the invoice synchronously.
            if (invoiceItemRepository.existsBySourceTypeAndSourceId(FinanceSourceType.APPOINTMENT, event.appointmentId())) {
                log.info("DEDUPLICATION: Invoice already exists for appointment {}, skipping auto-generation.", event.appointmentId());
                return;
            }

            log.info("NO DEDUPLICATION: Generating invoice for appointment {}...", event.appointmentId());

            Appointment appointment = appointmentRepository.findById(event.appointmentId())
                    .orElseThrow(() -> new RuntimeException("Appointment not found: " + event.appointmentId()));

            log.info("Found appointment {}, visitId: {}, generating invoice...", appointment.getId(), appointment.getVisitId());

            // 1. Get or Create Invoice
            Invoice invoice = invoiceService.getOrCreateInvoice(
                    event.hospitalId(),
                    event.patientId(),
                    appointment.getVisitId()
            );

            // 2. Get Consultation Fee
            BigDecimal fee = doctorHospitalDetailsRepository
                    .findByUserIdAndHospitalId(appointment.getDoctorId(), event.hospitalId())
                    .map(DoctorHospitalDetails::getConsultationFee)
                    .orElse(BigDecimal.ZERO);

            // 3. Add Consultation Item
            invoiceService.addItem(
                    invoice.getId(),
                    "Consultation - " + appointment.getDoctorName(),
                    InvoiceItemCategory.CONSULTATION,
                    BigDecimal.ONE,
                    fee,
                    FinanceSourceType.APPOINTMENT,
                    event.appointmentId(),
                    appointment.getDoctorId(),
                    appointment.getDoctorName()
            );

            log.info("Evolved Finance: Invoice {} created for appointment {}", invoice.getInvoiceNumber(), event.appointmentId());
        } catch (Exception e) {
            log.error("Evolved Finance: Failed to generate invoice for appointment {}", event.appointmentId(), e);
        }
    }

    @EventListener
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public void onPharmacySaleCompleted(PharmacySaleCompletedEvent event) {
        log.info("Evolved Finance: Generating invoice for pharmacy sale: {}", event.saleId());

        try {
            PharmacySale sale = pharmacySaleRepository.findById(event.saleId())
                    .orElseThrow(() -> new RuntimeException("Pharmacy sale not found"));

            // 1. Get or Create Invoice
            Invoice invoice = invoiceService.getOrCreateInvoice(
                    event.hospitalId(),
                    event.patientId(),
                    sale.getVisitId()
            );

            // 2. Add Pharmacy Items
            sale.getItems().forEach(item -> {
                invoiceService.addItem(
                        invoice.getId(),
                        "Medicine - " + item.getMedicine().getName(),
                        InvoiceItemCategory.PHARMACY,
                        BigDecimal.valueOf(item.getQuantity()),
                        item.getUnitPrice(),
                        FinanceSourceType.PHARMACY_SALE_ITEM,
                        item.getId(),
                        null,
                        null
                );
            });

            log.info("Evolved Finance: Invoice {} created for pharmacy sale {}", invoice.getInvoiceNumber(), event.saleId());
        } catch (Exception e) {
            log.error("Evolved Finance: Failed to generate invoice for pharmacy sale {}", event.saleId(), e);
        }
    }
}
