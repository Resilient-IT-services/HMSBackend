package com.res.hms.appointment.service;

import com.res.hms.appointment.dto.AppointmentDTO;
import com.res.hms.appointment.repository.AppointmentRepository;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.doctor.service.DoctorProfileService;
import com.res.hms.domain.Appointment;
import com.res.hms.domain.AppointmentStatus;
import com.res.hms.domain.DoctorProfile;
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
  private final DoctorProfileService doctorProfileService;
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

  @Transactional
  public AppointmentDTO updateVitals(UUID id, com.res.hms.appointment.dto.VitalsDTO vitalsDto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Appointment appointment =
        appointmentRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));

    com.res.hms.domain.Vitals vitals = appointment.getVitals();
    if (vitals == null) {
      vitals = new com.res.hms.domain.Vitals();
      vitals.setAppointment(appointment);
      appointment.setVitals(vitals);
    }
    vitals.setBloodPressure(vitalsDto.getBloodPressure());
    vitals.setHeartRate(vitalsDto.getHeartRate());
    vitals.setTemperature(vitalsDto.getTemperature());
    vitals.setWeight(vitalsDto.getWeight());

    appointmentRepository.saveAndFlush(appointment);
    return appointmentService.toDto(appointment);
  }

  @Transactional
  public AppointmentDTO updateConsultation(UUID id, AppointmentDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Appointment appointment =
        appointmentRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));

    if (dto.getChiefComplaints() != null) {
      appointment.setChiefComplaints(dto.getChiefComplaints());
    }
    if (dto.getDiagnosis() != null) {
      appointment.setDiagnosis(dto.getDiagnosis());
    }
    if (dto.getClinicalNotes() != null) {
      appointment.setClinicalNotes(dto.getClinicalNotes());
    }
    if (dto.getIcdCodes() != null) {
      appointment.setIcdCodes(dto.getIcdCodes());
    }

    appointmentRepository.saveAndFlush(appointment);
    return appointmentService.toDto(appointment);
  }

  private Appointment createAppointment(AppointmentDTO dto, UUID hospitalId) {
    Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
    DoctorProfile profile = doctorProfileService.requireDoctor(dto.getDoctorId(), hospitalId);

    Appointment appointment = new Appointment();
    appointment.setHospitalId(hospitalId);
    appointment.setPatient(patient);
    appointment.setDoctorProfile(profile);
    appointment.setStatus(dto.getStatus() != null ? dto.getStatus() : AppointmentStatus.SCHEDULED);
    appointment.setTokenNumber(generateNextToken(profile.getId(), dto.getAppointmentTime()));
    updateAppointmentFields(appointment, dto);
    return appointment;
  }

  private void updateAppointment(Appointment appointment, AppointmentDTO dto, UUID hospitalId) {
    boolean doctorChanged =
        dto.getDoctorId() != null
            && !appointment.getDoctorProfile().getId().equals(dto.getDoctorId());
    boolean dateChanged =
        dto.getAppointmentTime() != null
            && !appointment
                .getAppointmentTime()
                .toLocalDate()
                .equals(dto.getAppointmentTime().toLocalDate());

    if (doctorChanged) {
      DoctorProfile profile = doctorProfileService.requireDoctor(dto.getDoctorId(), hospitalId);
      appointment.setDoctorProfile(profile);
    }
    if (dto.getPatientId() != null && !appointment.getPatient().getId().equals(dto.getPatientId())) {
      Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
      appointment.setPatient(patient);
    }
    if (dto.getStatus() != null) {
      appointment.setStatus(dto.getStatus());
    }

    if (doctorChanged || (dateChanged && dto.getAppointmentTime() != null)) {
      appointment.setTokenNumber(
          generateNextToken(appointment.getDoctorProfile().getId(), dto.getAppointmentTime()));
    }

    updateAppointmentFields(appointment, dto);
  }

  private Integer generateNextToken(UUID doctorId, java.time.LocalDateTime appointmentTime) {
    java.time.LocalDate date = appointmentTime.toLocalDate();
    Integer lastToken = appointmentRepository.findMaxTokenByDoctorAndDate(doctorId, date);

    if (lastToken == null) {
      return 1;
    }

    return lastToken + 1;
  }

  private void updateAppointmentFields(Appointment appointment, AppointmentDTO dto) {
    if (dto.getAppointmentTime() != null) {
      appointment.setAppointmentTime(dto.getAppointmentTime());
    }
    appointment.setNotes(dto.getNotes());
    appointment.setChiefComplaints(dto.getChiefComplaints());
    appointment.setDiagnosis(dto.getDiagnosis());
    appointment.setClinicalNotes(dto.getClinicalNotes());
    if (dto.getIcdCodes() != null) {
      appointment.setIcdCodes(dto.getIcdCodes());
    }

    if (dto.getVitals() != null) {
      com.res.hms.domain.Vitals vitals = appointment.getVitals();
      if (vitals == null) {
        vitals = new com.res.hms.domain.Vitals();
        vitals.setAppointment(appointment);
        appointment.setVitals(vitals);
      }
      vitals.setBloodPressure(dto.getVitals().getBloodPressure());
      vitals.setHeartRate(dto.getVitals().getHeartRate());
      vitals.setTemperature(dto.getVitals().getTemperature());
      vitals.setWeight(dto.getVitals().getWeight());
    }
  }
}
