package com.res.hms.clinical.application.appointment.service;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest;
import com.res.hms.clinical.application.service.PatientVisitService;
import com.res.hms.clinical.domain.PatientVisit;
import com.res.hms.clinical.domain.VisitType;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.service.DomainEventPublisher;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.dto.RecordPaymentRequest;
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.application.service.PaymentService;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OPDEncounterOrchestrator {

  private final AppointmentAggregateService appointmentAggregateService;
  private final AppointmentService appointmentService;
  private final AppointmentRepository appointmentRepository;
  private final PatientVisitService patientVisitService;
  private final InvoiceService invoiceService;
  private final PaymentService paymentService;
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final DomainEventPublisher eventPublisher;
  private final HospitalContext hospitalContext;

  @Transactional
  public AppointmentDTO bookConsultationWithPayment(OPDEncounterRequest request, UUID collectedBy) {
    log.info("Starting atomic OPD booking for patient: {}", request.getPatientId());
    UUID hospitalId = hospitalContext.requireHospitalId();

    // 1. Create Patient Visit
    PatientVisit visit =
        patientVisitService.createVisit(hospitalId, request.getPatientId(), VisitType.OPD);
    log.info("Created visit: {}", visit.getId());

    // 2. Create Appointment
    AppointmentDTO appointmentDto = request.toAppointmentDTO(visit.getId());

    // Set initial status based on payment and vitals
    if (request.getPayment() == null) {
      appointmentDto.setStatus(com.res.hms.clinical.domain.AppointmentStatus.PAYMENT_PENDING);
    } else {
      if (request.getVitals() != null) {
        appointmentDto.setStatus(com.res.hms.clinical.domain.AppointmentStatus.VITALS_RECORDED);
      } else {
        appointmentDto.setStatus(com.res.hms.clinical.domain.AppointmentStatus.PAID);
      }
    }

    AppointmentDTO savedAppointment =
        appointmentAggregateService.saveAppointmentAggregate(appointmentDto);
    log.info("Created appointment: {}", savedAppointment.getId());

    // 3. Create or Get Invoice (Finance Service)
    Invoice invoice =
        invoiceService.getOrCreateInvoice(hospitalId, request.getPatientId(), visit.getId());

    // Fetch consultation fee
    BigDecimal fee =
        doctorHospitalDetailsRepository
            .findByUserIdAndHospitalId(request.getDoctorId(), hospitalId)
            .map(DoctorHospitalDetails::getConsultationFee)
            .orElse(BigDecimal.ZERO);

    invoiceService.addItem(
        invoice.getId(),
        "Consultation - " + savedAppointment.getDoctorName(),
        InvoiceItemCategory.CONSULTATION,
        BigDecimal.ONE,
        fee,
        FinanceSourceType.APPOINTMENT,
        savedAppointment.getId(),
        request.getDoctorId(),
        savedAppointment.getDoctorName());

    // 4. Record Payment if provided
    if (request.getPayment() != null) {
      RecordPaymentRequest paymentRequest = new RecordPaymentRequest();
      paymentRequest.setPatientId(request.getPatientId());
      paymentRequest.setVisitId(visit.getId());
      paymentRequest.setAmount(request.getPayment().getAmount());
      paymentRequest.setPaymentMode(request.getPayment().getMode());
      paymentRequest.setReferenceNumber(request.getPayment().getReferenceNumber());
      paymentRequest.setInvoiceId(invoice.getId());

      paymentService.recordPayment(hospitalId, paymentRequest, collectedBy);
      log.info("Recorded payment for invoice: {}", invoice.getInvoiceNumber());
    }

    return savedAppointment;
  }
}
