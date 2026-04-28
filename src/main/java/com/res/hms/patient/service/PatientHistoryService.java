package com.res.hms.patient.service;

import com.res.hms.appointment.repository.AppointmentRepository;
import com.res.hms.appointment.service.AppointmentService;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.PatientHistoryDTO;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientHistoryService {

  private final PatientService patientService;
  private final AppointmentRepository appointmentRepository;
  private final AppointmentService appointmentService;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public PatientHistoryDTO getFullHistory(UUID patientId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Patient patient = patientService.requirePatient(patientId, hospitalId);

    return PatientHistoryDTO.builder()
        .patient(patientService.toDto(patient))
        .visits(
            appointmentRepository
                .findByPatientIdAndHospitalIdOrderByAppointmentTimeDesc(patientId, hospitalId)
                .stream()
                .map(appointmentService::toDto)
                .collect(Collectors.toList()))
        .build();
  }
}
