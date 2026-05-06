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
import com.res.hms.vital.dto.VitalsDTO;
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
  private final com.res.hms.vital.service.VitalValidationService vitalValidationService;
  private final com.res.hms.appointment.mapper.VitalsMapper vitalsMapper;

  @Transactional
  public AppointmentDTO saveAppointmentAggregate(AppointmentDTO dto) {
    log.info("Starting aggregate save for appointment");
    UUID hospitalId = hospitalContext.requireHospitalId();

    if (dto.getVitals() != null) {
      vitalValidationService.validate(dto.getVitals());
    }

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
  public AppointmentDTO updateVitals(UUID id, VitalsDTO vitalsDto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    vitalValidationService.validate(vitalsDto);

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
    vitalsMapper.updateEntity(vitalsDto, vitals);

    return appointmentService.toDto(appointmentRepository.save(appointment));
  }

  @Transactional
  public AppointmentDTO updateConsultation(UUID id, AppointmentDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    Appointment appointment =
        appointmentRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));

    appointment.setChiefComplaints(dto.getChiefComplaints());
    appointment.setDiagnosis(dto.getDiagnosis());
    appointment.setClinicalNotes(dto.getClinicalNotes());
    appointment.setIcdCodes(dto.getIcdCodes());
    appointment.setStatus(AppointmentStatus.COMPLETED);

    return appointmentService.toDto(appointmentRepository.save(appointment));
  }

  private void mapVitalsDtoToEntity(VitalsDTO dto, com.res.hms.domain.Vitals entity) {
    vitalsMapper.updateEntity(dto, entity);
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
    boolean doctorChanged = !appointment.getDoctorProfile().getId().equals(dto.getDoctorId());
    boolean dateChanged =
        !appointment
            .getAppointmentTime()
            .toLocalDate()
            .equals(dto.getAppointmentTime().toLocalDate());

    if (doctorChanged) {
      DoctorProfile profile = doctorProfileService.requireDoctor(dto.getDoctorId(), hospitalId);
      appointment.setDoctorProfile(profile);
    }
    if (!appointment.getPatient().getId().equals(dto.getPatientId())) {
      Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
      appointment.setPatient(patient);
    }
    if (dto.getStatus() != null) {
      appointment.setStatus(dto.getStatus());
    }

    if (doctorChanged || dateChanged) {
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
    appointment.setAppointmentTime(dto.getAppointmentTime());
    appointment.setNotes(dto.getNotes());

    if (dto.getVitals() != null) {
      com.res.hms.domain.Vitals vitals = appointment.getVitals();
      if (vitals == null) {
        vitals = new com.res.hms.domain.Vitals();
        vitals.setAppointment(appointment);
        appointment.setVitals(vitals);
      }
      vitalsMapper.updateEntity(dto.getVitals(), vitals);
    }
  }
}
