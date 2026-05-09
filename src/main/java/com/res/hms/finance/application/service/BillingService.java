package com.res.hms.finance.application.service;

import com.res.hms.clinical.application.patient.service.PatientService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.dto.BillingDTO;
import com.res.hms.finance.application.dto.BillingItemDTO;
import com.res.hms.finance.domain.Billing;
import com.res.hms.finance.domain.BillingItem;
import com.res.hms.finance.infrastructure.repository.BillingRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingService {

  private final BillingRepository billingRepository;
  private final AppointmentRepository appointmentRepository;
  private final PatientService patientService;
  private final HospitalContext hospitalContext;

  @Transactional
  public BillingDTO createBilling(BillingDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    Billing billing = new Billing();
    billing.setHospitalId(hospitalId);

    if (dto.getAppointmentId() != null) {
      Appointment appointment =
          appointmentRepository
              .findByIdAndHospitalId(dto.getAppointmentId(), hospitalId)
              .orElseThrow(
                  () ->
                      new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));
      
      billing.setAppointmentId(appointment.getId());
      billing.setPatientId(appointment.getPatientId());
      if (appointment.getDoctorId() != null) {
        billing.setDoctorId(appointment.getDoctorId());
        billing.setDoctorName(appointment.getDoctorName());
      }
    } else {
      billing.setPatientId(dto.getPatientId());
    }

    billing.setSourceType(dto.getSourceType());
    billing.setStatus(dto.getStatus());
    billing.setTotalAmount(dto.getTotalAmount());
    billing.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO);
    billing.setPaymentMethod(dto.getPaymentMethod());

    if (dto.getItems() != null) {
      for (BillingItemDTO itemDto : dto.getItems()) {
        BillingItem item = new BillingItem();
        item.setHospitalId(hospitalId);
        item.setBilling(billing);
        item.setItemName(itemDto.getItemName());
        item.setAmount(itemDto.getAmount());
        item.setQuantity(itemDto.getQuantity());
        item.setSubtotal(itemDto.getSubtotal());
        billing.getItems().add(item);
      }
    }

    return toDto(billingRepository.save(billing));
  }

  @Transactional(readOnly = true)
  public BillingDTO getByAppointmentId(UUID appointmentId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return billingRepository
        .findByAppointmentIdAndHospitalId(appointmentId, hospitalId)
        .map(this::toDto)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Bill not found", HttpStatus.NOT_FOUND));
  }

  @Transactional
  public BillingDTO updateBillingStatus(
      UUID billingId,
      com.res.hms.finance.domain.BillingStatus status,
      String paymentMethod,
      BigDecimal discount) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Billing billing =
        billingRepository
            .findByIdAndHospitalId(billingId, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Bill not found", HttpStatus.NOT_FOUND));

    billing.setStatus(status);
    if (paymentMethod != null) {
      billing.setPaymentMethod(paymentMethod);
    }
    if (discount != null) {
      billing.setDiscount(discount);
    }

    // If billing is paid, and it's linked to an appointment with PAYMENT_PENDING status, update it
    if (status == com.res.hms.finance.domain.BillingStatus.PAID
        && billing.getAppointmentId() != null) {
      appointmentRepository.findByIdAndHospitalId(billing.getAppointmentId(), hospitalId)
          .ifPresent(appointment -> {
            if (appointment.getStatus() == com.res.hms.clinical.domain.AppointmentStatus.PAYMENT_PENDING) {
              appointment.setStatus(com.res.hms.clinical.domain.AppointmentStatus.SCHEDULED);
              appointmentRepository.save(appointment);
            }
          });
    }

    return toDto(billingRepository.save(billing));
  }

  public BillingDTO toDto(Billing billing) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    String patientName = "Unknown";
    try {
        patientName = patientService.get(billing.getPatientId()).getFullName();
    } catch (Exception e) {
        // Log or handle missing patient
    }

    return BillingDTO.builder()
        .id(billing.getId())
        .patientId(billing.getPatientId())
        .patientName(patientName)
        .appointmentId(billing.getAppointmentId())
        .sourceType(billing.getSourceType())
        .status(billing.getStatus())
        .totalAmount(billing.getTotalAmount())
        .discount(billing.getDiscount())
        .paymentMethod(billing.getPaymentMethod())
        .createdAt(billing.getCreatedAt())
        .items(
            billing.getItems().stream()
                .map(
                    item ->
                        BillingItemDTO.builder()
                            .id(item.getId())
                            .itemName(item.getItemName())
                            .amount(item.getAmount())
                            .quantity(item.getQuantity())
                            .subtotal(item.getSubtotal())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }
}
