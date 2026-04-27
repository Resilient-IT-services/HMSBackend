package com.res.hms.appointment.service;

import com.res.hms.appointment.dto.AppointmentDTO;
import com.res.hms.appointment.repository.AppointmentRepository;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Appointment;
import com.res.hms.domain.AppointmentStatus;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Patient;
import com.res.hms.patient.service.PatientService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentAggregateService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentService appointmentService;
  private final PatientService patientService;
  private final DoctorService doctorService;
  private final HospitalContext hospitalContext;

  @Transactional
  public AppointmentDTO saveAppointmentAggregate(AppointmentDTO dto) {
    log.info("Starting aggregate save for appointment");
    UUID hospitalId = hospitalContext.requireHospitalId();

    Appointment appointment;
    if (dto.getId() == null) {
      appointment = createAppointment(dto, hospitalId);
    } else {
      appointment =
          appointmentRepository
              .findByIdAndHospitalId(dto.getId(), hospitalId)
              .orElseThrow(
                  () ->
                      new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));
      updateAppointment(appointment, dto, hospitalId);
    }

    appointmentRepository.saveAndFlush(appointment);
    return appointmentService.toDto(appointment);
  }

  private Appointment createAppointment(AppointmentDTO dto, UUID hospitalId) {
    Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
    Doctor doctor = doctorService.requireDoctor(dto.getDoctorId(), hospitalId);

    Appointment appointment = new Appointment();
    appointment.setHospitalId(hospitalId);
    appointment.setPatient(patient);
    appointment.setDoctor(doctor);
    appointment.setStatus(dto.getStatus() != null ? dto.getStatus() : AppointmentStatus.SCHEDULED);
    updateAppointmentFields(appointment, dto);
    return appointment;
  }

  private void updateAppointment(Appointment appointment, AppointmentDTO dto, UUID hospitalId) {
    if (!appointment.getPatient().getId().equals(dto.getPatientId())) {
      Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
      appointment.setPatient(patient);
    }
    if (!appointment.getDoctor().getId().equals(dto.getDoctorId())) {
      Doctor doctor = doctorService.requireDoctor(dto.getDoctorId(), hospitalId);
      appointment.setDoctor(doctor);
    }
    if (dto.getStatus() != null) {
      appointment.setStatus(dto.getStatus());
    }
    updateAppointmentFields(appointment, dto);
  }

  private void updateAppointmentFields(Appointment appointment, AppointmentDTO dto) {
    appointment.setAppointmentTime(dto.getAppointmentTime());
    appointment.setNotes(dto.getNotes());

    if (dto.getVitals() != null) {
      com.res.hms.domain.Vitals vitals = appointment.getVitals();
      if (vitals == null) {
        vitals = new com.res.hms.domain.Vitals();
        vitals.setAppointment(appointment);
        vitals.setHospitalId(appointment.getHospitalId());
        appointment.setVitals(vitals);
      }
      vitals.setBloodPressure(dto.getVitals().getBloodPressure());
      vitals.setHeartRate(dto.getVitals().getHeartRate());
      vitals.setTemperature(dto.getVitals().getTemperature());
      vitals.setWeight(dto.getVitals().getWeight());
    }
  }
}
