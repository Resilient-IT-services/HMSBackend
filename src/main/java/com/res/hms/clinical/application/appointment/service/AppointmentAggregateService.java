package com.res.hms.clinical.application.appointment.service;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.application.patient.service.PatientService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.iam.application.service.DoctorProfileService;
import com.res.hms.iam.domain.DoctorProfile;
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
  private final com.res.hms.clinical.application.vital.service.VitalValidationService
      vitalValidationService;
  private final com.res.hms.clinical.application.service.PatientVisitService patientVisitService;

  private final com.res.hms.core.service.DomainEventPublisher eventPublisher;

  @Transactional
  public AppointmentDTO saveAppointmentAggregate(AppointmentDTO dto) {
    log.info("Starting aggregate save for appointment");
    UUID hospitalId = hospitalContext.requireHospitalId();
    
    boolean alreadyBooked;
    if (dto.getId() == null) {
      alreadyBooked = appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentTimeAndStatusNot(
          dto.getPatientId(), dto.getDoctorId(), dto.getAppointmentTime(), AppointmentStatus.CANCELLED);
    } else {
      alreadyBooked = appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentTimeAndIdNotAndStatusNot(
          dto.getPatientId(), dto.getDoctorId(), dto.getAppointmentTime(), dto.getId(), AppointmentStatus.CANCELLED);
    }

    if (alreadyBooked) {
      throw new ApiException("APPOINTMENT_ALREADY_EXISTS", 
          "Appointment already exists for this patient with the same doctor at the selected time", 
          HttpStatus.CONFLICT);
    }

    if (dto.getVitals() != null) {
      vitalValidationService.validate(dto.getVitals());
    }

    Appointment appointment;
    boolean isNew = dto.getId() == null;
    if (isNew) {
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

    if (isNew) {
      eventPublisher.publish(
          "appointment",
          appointment.getId(),
          new com.res.hms.clinical.application.event.AppointmentBookedEvent(
              appointment.getId(), appointment.getPatientId(), hospitalId));
    }

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

    com.res.hms.clinical.domain.Vitals vitals = appointment.getVitals();
    if (vitals == null) {
      vitals = new com.res.hms.clinical.domain.Vitals();
      vitals.setHospitalId(hospitalId);
      vitals.setAppointment(appointment);
      appointment.setVitals(vitals);
    }
    mapVitalsDtoToEntity(vitalsDto, vitals);

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

  private void mapVitalsDtoToEntity(VitalsDTO dto, com.res.hms.clinical.domain.Vitals entity) {
    entity.setBloodPressure(dto.getBloodPressure());
    entity.setSystolic(dto.getSystolic());
    entity.setDiastolic(dto.getDiastolic());
    entity.setHeartRate(dto.getHeartRate());
    entity.setOxygenLevel(dto.getOxygenLevel());
    entity.setTemperature(dto.getTemperature());
    entity.setWeight(dto.getWeight());
  }

  private Appointment createAppointment(AppointmentDTO dto, UUID hospitalId) {
    Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
    DoctorProfile profile = doctorProfileService.requireDoctor(dto.getDoctorId(), hospitalId);

    Appointment appointment = new Appointment();
    appointment.setHospitalId(hospitalId);
    appointment.setPatientId(patient.getId());
    appointment.setPatientName(patient.getFullName());
    appointment.setDoctorId(profile.getId());
    appointment.setDoctorName(profile.getFullName());
    appointment.setStatus(dto.getStatus() != null ? dto.getStatus() : AppointmentStatus.SCHEDULED);
    appointment.setTokenNumber(generateNextToken(profile.getId(), dto.getAppointmentTime()));
    
    if (dto.getVisitId() != null) {
      appointment.setVisitId(dto.getVisitId());
    } else {
      com.res.hms.clinical.domain.PatientVisit visit = patientVisitService.createVisit(
          hospitalId, patient.getId(), com.res.hms.clinical.domain.VisitType.OPD);
      appointment.setVisitId(visit.getId());
    }

    updateAppointmentFields(appointment, dto, hospitalId);
    return appointment;
  }

  private void updateAppointment(Appointment appointment, AppointmentDTO dto, UUID hospitalId) {
    boolean doctorChanged = !appointment.getDoctorId().equals(dto.getDoctorId());
    boolean dateChanged =
        !appointment
            .getAppointmentTime()
            .toLocalDate()
            .equals(dto.getAppointmentTime().toLocalDate());

    if (doctorChanged) {
      DoctorProfile profile = doctorProfileService.requireDoctor(dto.getDoctorId(), hospitalId);
      appointment.setDoctorId(profile.getId());
      appointment.setDoctorName(profile.getFullName());
    }
    if (!appointment.getPatientId().equals(dto.getPatientId())) {
      Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
      appointment.setPatientId(patient.getId());
      appointment.setPatientName(patient.getFullName());
    }
    if (dto.getStatus() != null) {
      appointment.setStatus(dto.getStatus());
    }

    if (doctorChanged || dateChanged) {
      appointment.setTokenNumber(
          generateNextToken(appointment.getDoctorId(), dto.getAppointmentTime()));
    }

    updateAppointmentFields(appointment, dto, hospitalId);
  }

  private Integer generateNextToken(UUID doctorId, java.time.LocalDateTime appointmentTime) {
    java.time.LocalDate date = appointmentTime.toLocalDate();
    Integer lastToken = appointmentRepository.findMaxTokenByDoctorAndDate(doctorId, date);

    if (lastToken == null) {
      return 1;
    }

    return lastToken + 1;
  }

  private void updateAppointmentFields(
      Appointment appointment, AppointmentDTO dto, UUID hospitalId) {
    appointment.setAppointmentTime(dto.getAppointmentTime());
    appointment.setNotes(dto.getNotes());

    if (dto.getVitals() != null) {
      com.res.hms.clinical.domain.Vitals vitals = appointment.getVitals();
      if (vitals == null) {
        vitals = new com.res.hms.clinical.domain.Vitals();
        vitals.setHospitalId(hospitalId);
        vitals.setAppointment(appointment);
        appointment.setVitals(vitals);
      }
      vitals.setBloodPressure(dto.getVitals().getBloodPressure());
      vitals.setHeartRate(dto.getVitals().getHeartRate());
      vitals.setTemperature(dto.getVitals().getTemperature());
      vitals.setWeight(dto.getVitals().getWeight());
      vitals.setOxygenLevel(dto.getVitals().getOxygenLevel());
    }
  }
}

