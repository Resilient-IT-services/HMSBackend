package com.res.hms.appointment.service;

import com.res.hms.appointment.dto.AppointmentDTO;
import com.res.hms.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.appointment.repository.AppointmentRepository;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Appointment;
import com.res.hms.prescription.service.PrescriptionService;
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
  public PageResponse<AppointmentDTO> list(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      String searchPattern = "%" + search.trim().toLowerCase() + "%";
      return PageResponse.from(
          appointmentRepository.search(hospitalId, searchPattern, pageRequest).map(this::toDto));
    }

    return PageResponse.from(
        appointmentRepository
            .findAllByHospitalIdOrderByAppointmentTimeDesc(hospitalId, pageRequest)
            .map(this::toDto));
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
    return AppointmentDTO.builder()
        .id(appointment.getId())
        .patientId(appointment.getPatient().getId())
        .patientName(appointment.getPatient().getFullName())
        .doctorId(appointment.getDoctorProfile().getId())
        .doctorName(appointment.getDoctorProfile().getFullName())
        .appointmentTime(appointment.getAppointmentTime())
        .status(appointment.getStatus())
        .tokenNumber(appointment.getTokenNumber())
        .chiefComplaints(appointment.getChiefComplaints())
        .diagnosis(appointment.getDiagnosis())
        .clinicalNotes(appointment.getClinicalNotes())
        .icdCodes(appointment.getIcdCodes())
        .notes(appointment.getNotes())
        .vitals(toVitalsDto(appointment.getVitals()))
        .prescription(
            appointment.getPrescription() != null
                ? prescriptionService.toDto(appointment.getPrescription())
                : null)
        .build();
  }

  private com.res.hms.appointment.dto.VitalsDTO toVitalsDto(com.res.hms.domain.Vitals vitals) {
    if (vitals == null) {
      return null;
    }
    return com.res.hms.appointment.dto.VitalsDTO.builder()
        .bloodPressure(vitals.getBloodPressure())
        .heartRate(vitals.getHeartRate())
        .temperature(vitals.getTemperature())
        .weight(vitals.getWeight())
        .build();
  }
}
