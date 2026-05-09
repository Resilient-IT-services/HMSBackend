package com.res.hms.clinical.application.appointment.service;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.clinical.application.prescription.service.PrescriptionService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.audit.Auditable;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Lazy private final PrescriptionService prescriptionService;

  @Transactional(readOnly = true)
  public AppointmentDTO getById(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return appointmentRepository
        .findByIdAndHospitalId(id, hospitalId)
        .map(this::toDto)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public PageResponse<AppointmentDTO> list(
      int page, int size, String search, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      String searchPattern = "%" + search.trim().toLowerCase() + "%";
      return PageResponse.from(
          appointmentRepository.search(hospitalId, searchPattern, pageRequest).map(this::toDto));
    }

    if (date != null) {
      return PageResponse.from(
          appointmentRepository
              .findAllByHospitalIdAndDate(hospitalId, date, pageRequest)
              .map(this::toDto));
    }

    return PageResponse.from(
        appointmentRepository
            .findAllByHospitalIdOrderByAppointmentTimeDesc(hospitalId, pageRequest)
            .map(this::toDto));
  }

  @Transactional(readOnly = true)
  public com.res.hms.clinical.application.appointment.dto.AppointmentStatsDTO getStats(
      java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    java.util.List<Object[]> results = appointmentRepository.getStatsByDate(hospitalId, date);

    java.util.Map<com.res.hms.clinical.domain.AppointmentStatus, Long> byStatus =
        new java.util.EnumMap<>(com.res.hms.clinical.domain.AppointmentStatus.class);
    long total = 0;

    for (Object[] result : results) {
      com.res.hms.clinical.domain.AppointmentStatus status =
          (com.res.hms.clinical.domain.AppointmentStatus) result[0];
      Long count = (Long) result[1];
      byStatus.put(status, count);
      total += count;
    }

    return com.res.hms.clinical.application.appointment.dto.AppointmentStatsDTO.builder()
        .total(total)
        .byStatus(byStatus)
        .build();
  }

  @Transactional
  @Auditable(entity = "appointment", action = "UPDATE_STATUS")
  public AppointmentDTO updateStatus(UUID id, UpdateAppointmentStatusRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Appointment appointment =
        resourceLookup.findRequired(
            id, hospitalId, appointmentRepository::findByIdAndHospitalId, "Appointment");

    appointment.setStatus(request.status());
    return toDto(appointmentRepository.save(appointment));
  }

  public AppointmentDTO toDto(Appointment appointment) {
    if (appointment == null) {
      return null;
    }

    AppointmentDTO.AppointmentDTOBuilder builder =
        AppointmentDTO.builder()
            .id(appointment.getId())
            .appointmentTime(appointment.getAppointmentTime())
            .status(appointment.getStatus())
            .tokenNumber(appointment.getTokenNumber())
            .sessionPeriod(appointment.getSessionPeriod())
            .chiefComplaints(appointment.getChiefComplaints())
            .diagnosis(appointment.getDiagnosis())
            .clinicalNotes(appointment.getClinicalNotes())
            .icdCodes(appointment.getIcdCodes())
            .notes(appointment.getNotes())
            .vitals(toVitalsDto(appointment.getVitals()))
            .prescription(
                appointment.getPrescription() != null
                    ? prescriptionService.toDto(appointment.getPrescription())
                    : null);

    if (appointment.getPatientId() != null) {
      builder
          .patientId(appointment.getPatientId())
          .patientName(appointment.getPatientName());
    }

    if (appointment.getDoctorId() != null) {
      builder
          .doctorId(appointment.getDoctorId())
          .doctorName(appointment.getDoctorName());
    }

    return builder.build();
  }

  private com.res.hms.clinical.application.appointment.dto.VitalsDTO toVitalsDto(
      com.res.hms.clinical.domain.Vitals vitals) {
    if (vitals == null) {
      return null;
    }
    return com.res.hms.clinical.application.appointment.dto.VitalsDTO.builder()
        .bloodPressure(vitals.getBloodPressure())
        .systolic(vitals.getSystolic())
        .diastolic(vitals.getDiastolic())
        .heartRate(vitals.getHeartRate())
        .oxygenLevel(vitals.getOxygenLevel())
        .temperatureUnit(vitals.getTemperatureUnit())
        .temperature(vitals.getTemperature())
        .weightUnit(vitals.getWeightUnit())
        .weight(vitals.getWeight())
        .build();
  }
}
