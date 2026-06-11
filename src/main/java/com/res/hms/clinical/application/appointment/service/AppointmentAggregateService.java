package com.res.hms.clinical.application.appointment.service;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.application.patient.service.PatientService;
import com.res.hms.clinical.application.prescription.service.PrescriptionService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.iam.application.service.DoctorProfileService;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.service.LabOrderService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
  private final PrescriptionService prescriptionService;
  private final HospitalContext hospitalContext;
  private final com.res.hms.clinical.application.vital.service.VitalValidationService
      vitalValidationService;
  private final com.res.hms.clinical.application.service.PatientVisitService patientVisitService;
  private final com.res.hms.hospital.infrastructure.repository.HospitalRepository
      hospitalRepository;
  private final com.res.hms.core.service.DomainEventPublisher eventPublisher;
  private final AppointmentValidationHelper validationHelper;
  private final LabOrderService labOrderService;

  @Transactional
  public AppointmentDTO saveAppointmentAggregate(AppointmentDTO dto) {
    log.info("Starting aggregate save for appointment");
    UUID hospitalId = hospitalContext.requireHospitalId();
    ZoneId zoneId = validationHelper.getHospitalZoneId(hospitalId);

    Appointment existing = null;
    if (dto.getId() != null) {
      existing = appointmentRepository.findById(dto.getId()).orElse(null);
    }

    validateStatusForUpdate(dto, existing);

    // Validation: Prevent past bookings
    boolean isNew = dto.getId() == null;
    if (isNew
        && dto.getAppointmentTime().isBefore(java.time.OffsetDateTime.now().minusMinutes(1))) {
      throw new ApiException(
          "PAST_BOOKING_DISALLOWED",
          "Cannot book an appointment in the past.",
          HttpStatus.BAD_REQUEST);
    }

    ZonedDateTime appointmentZdt = dto.getAppointmentTime().atZoneSameInstant(zoneId);
    LocalDate appointmentDate = appointmentZdt.toLocalDate();
    Instant[] range = validationHelper.getDayRange(appointmentDate, zoneId);

    validateDoctorOverlap(dto, range, isNew);

    if (dto.getVitals() != null) {
      vitalValidationService.validate(dto.getVitals());
    }

    Appointment appointment;
    if (isNew) {
      appointment = createAppointment(dto, hospitalId, zoneId);
    } else {
      appointment =
          appointmentRepository
              .findByIdAndHospitalId(dto.getId(), hospitalId)
              .orElseThrow(
                  () ->
                      new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));
      updateAppointment(appointment, dto, hospitalId, zoneId);
    }

    validationHelper.validateAppointmentProximity(dto, hospitalId, zoneId, range);

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

  private void validateStatusForUpdate(AppointmentDTO dto, Appointment existing) {
    if (existing != null
        && (existing.getStatus() == AppointmentStatus.PRESCRIPTION_FINALIZED
            || existing.getStatus() == AppointmentStatus.COMPLETED)) {
      if (dto.getStatus() == existing.getStatus() || dto.getStatus() == null) {
        log.warn("Attempt to update clinical data of a finalized appointment: {}", dto.getId());
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Finalized consultations are read-only. Reset prescription to make changes.",
            HttpStatus.BAD_REQUEST);
      }
    }

    if (existing != null && hospitalContext.isReceptionist()) {
      if (existing.getStatus() == AppointmentStatus.CONSULTATION_STARTED
          || existing.getStatus() == AppointmentStatus.PRESCRIPTION_FINALIZED
          || existing.getStatus() == AppointmentStatus.COMPLETED) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Receptionists cannot modify an appointment once consultation has started.",
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  private void validateDoctorOverlap(AppointmentDTO dto, Instant[] range, boolean isNew) {
    boolean doctorOverlap;
    if (isNew) {
      doctorOverlap =
          appointmentRepository.existsByPatientIdAndDoctorIdAndDateRangeAndStatusNot(
              dto.getPatientId(),
              dto.getDoctorId(),
              range[0],
              range[1],
              AppointmentStatus.CANCELLED);
    } else {
      doctorOverlap =
          appointmentRepository.existsByPatientIdAndDoctorIdAndDateRangeAndIdNotAndStatusNot(
              dto.getPatientId(),
              dto.getDoctorId(),
              range[0],
              range[1],
              dto.getId(),
              AppointmentStatus.CANCELLED);
    }

    if (doctorOverlap) {
      throw new ApiException(
          "DOCTOR_ALREADY_BOOKED_FOR_DAY",
          "Patient already has an appointment with this doctor on the selected date",
          HttpStatus.BAD_REQUEST);
    }
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

    if (appointment.getStatus() == AppointmentStatus.PRESCRIPTION_FINALIZED
        || appointment.getStatus() == AppointmentStatus.COMPLETED) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Cannot update vitals for finalized appointments.",
          HttpStatus.BAD_REQUEST);
    }

    com.res.hms.clinical.domain.Vitals vitals = appointment.getVitals();
    if (vitals == null) {
      vitals = new com.res.hms.clinical.domain.Vitals();
      vitals.setHospitalId(hospitalId);
      vitals.setAppointment(appointment);
      appointment.setVitals(vitals);
    }
    validationHelper.mapVitalsDtoToEntity(vitalsDto, vitals);

    if (appointment.getStatus() == AppointmentStatus.PAID
        || appointment.getStatus() == AppointmentStatus.SCHEDULED) {
      appointment.setStatus(AppointmentStatus.VITALS_RECORDED);
    }
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

    if (appointment.getStatus() == AppointmentStatus.PRESCRIPTION_FINALIZED
        || appointment.getStatus() == AppointmentStatus.COMPLETED) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Cannot update consultation for finalized appointments.",
          HttpStatus.BAD_REQUEST);
    }

    if (appointment.getStatus() != AppointmentStatus.VITALS_RECORDED
        && appointment.getStatus() != AppointmentStatus.CONSULTATION_STARTED) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Consultation can only be started after vitals are recorded.",
          HttpStatus.BAD_REQUEST);
    }

    if (appointment.getStatus() == AppointmentStatus.VITALS_RECORDED) {
      ensureNoOtherConsultationRunning(appointment);
    }

    appointment.setChiefComplaints(dto.getChiefComplaints());
    appointment.setDiagnosis(dto.getDiagnosis());
    appointment.setClinicalNotes(dto.getClinicalNotes());
    appointment.setIcdCodes(dto.getIcdCodes());
    appointment.setStatus(AppointmentStatus.CONSULTATION_STARTED);

    if (dto.getPrescription() != null) {
      prescriptionService.saveOrUpdatePrescription(appointment, dto.getPrescription());
    }

    return appointmentService.toDto(appointmentRepository.save(appointment));
  }

  @Transactional
  public AppointmentDTO resetPrescription(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Appointment appointment =
        appointmentRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));

    com.res.hms.audit.domain.AuditLog logEvent = new com.res.hms.audit.domain.AuditLog();
    logEvent.setHospitalId(hospitalId);
    logEvent.setEntityName(com.res.hms.audit.domain.DomainEntity.APPOINTMENT);
    logEvent.setEntityId(id);
    logEvent.setAction(com.res.hms.audit.domain.AuditAction.UPDATE);
    com.res.hms.audit.application.dto.AuditDetail details =
        new com.res.hms.audit.application.dto.AuditDetail();
    java.util.Map<String, Object> changes = new java.util.HashMap<>();
    changes.put("action", "ADMIN_RESET");
    changes.put("reason", "Appointment prescription reset by admin");
    details.setAfter(changes);
    logEvent.setDetails(details);
    eventPublisher.publish("audit", id, logEvent);

    appointment.setStatus(AppointmentStatus.VITALS_RECORDED);
    return appointmentService.toDto(appointmentRepository.save(appointment));
  }

  @Transactional
  public AppointmentDTO startNextConsultation(UUID doctorId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    ZoneId zoneId = validationHelper.getHospitalZoneId(hospitalId);
    LocalDate todayDate = ZonedDateTime.now(zoneId).toLocalDate();
    Instant[] range = validationHelper.getDayRange(todayDate, zoneId);

    java.util.List<Appointment> runningAppointments =
        appointmentRepository.findRunningConsultationsForUpdateByDateRange(
            hospitalId, doctorId, range[0], range[1]);

    if (!runningAppointments.isEmpty()) {
      throw new ApiException(
          "CONSULTATION_RUNNING", "One consultation is already in progress", HttpStatus.CONFLICT);
    }

    java.util.List<Appointment> waitingAppointments =
        appointmentRepository.findWaitingAppointmentsForUpdateByDateRange(
            hospitalId,
            doctorId,
            range[0],
            range[1],
            org.springframework.data.domain.PageRequest.of(0, 1));

    if (waitingAppointments.isEmpty()) {
      throw new ApiException(
          "NO_PATIENTS",
          "No paid or vitals-recorded patients are available in the queue",
          HttpStatus.NOT_FOUND);
    }

    Appointment nextAppointment = waitingAppointments.get(0);
    nextAppointment.setStatus(AppointmentStatus.CONSULTATION_STARTED);
    return appointmentService.toDto(appointmentRepository.saveAndFlush(nextAppointment));
  }

  private Appointment createAppointment(AppointmentDTO dto, UUID hospitalId, ZoneId zoneId) {
    Patient patient = patientService.requirePatient(dto.getPatientId(), hospitalId);
    DoctorProfile profile = doctorProfileService.requireDoctor(dto.getDoctorId(), hospitalId);

    Appointment appointment = new Appointment();
    appointment.setHospitalId(hospitalId);
    appointment.setPatientId(patient.getId());
    appointment.setPatientName(patient.getFullName());
    appointment.setDoctorId(profile.getId());
    appointment.setDoctorName(profile.getFullName());
    appointment.setStatus(dto.getStatus() != null ? dto.getStatus() : AppointmentStatus.SCHEDULED);
    appointment.setTokenNumber(
        validationHelper.generateNextToken(
            profile.getId(), dto.getAppointmentTime().toInstant(), zoneId));

    if (dto.getVisitId() != null) {
      appointment.setVisitId(dto.getVisitId());
    } else {
      com.res.hms.clinical.domain.PatientVisit visit =
          patientVisitService.createVisit(
              hospitalId, patient.getId(), com.res.hms.clinical.domain.VisitType.OPD);
      appointment.setVisitId(visit.getId());
    }

    updateAppointmentFields(appointment, dto, hospitalId, zoneId);
    return appointment;
  }

  private void updateAppointment(
      Appointment appointment, AppointmentDTO dto, UUID hospitalId, ZoneId zoneId) {
    boolean doctorChanged = !appointment.getDoctorId().equals(dto.getDoctorId());
    boolean dateChanged =
        !appointment
            .getAppointmentTime()
            .atZone(zoneId)
            .toLocalDate()
            .equals(dto.getAppointmentTime().atZoneSameInstant(zoneId).toLocalDate());

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
          validationHelper.generateNextToken(
              appointment.getDoctorId(), dto.getAppointmentTime().toInstant(), zoneId));
    }

    updateAppointmentFields(appointment, dto, hospitalId, zoneId);
  }

  private void updateAppointmentFields(
      Appointment appointment, AppointmentDTO dto, UUID hospitalId, ZoneId zoneId) {
    java.time.OffsetDateTime appointmentTime = dto.getAppointmentTime();
    appointment.setAppointmentTime(appointmentTime.toInstant());
    ZonedDateTime zdt = appointmentTime.atZoneSameInstant(zoneId);
    appointment.setAppointmentDate(zdt.toLocalDate());
    appointment.setAppointmentSlotTime(zdt.toLocalTime());
    appointment.setNotes(dto.getNotes());

    if (dto.getVitals() != null) {
      com.res.hms.clinical.domain.Vitals vitals = appointment.getVitals();
      if (vitals == null) {
        vitals = new com.res.hms.clinical.domain.Vitals();
        vitals.setHospitalId(hospitalId);
        vitals.setAppointment(appointment);
        appointment.setVitals(vitals);
      }
      validationHelper.mapVitalsDtoToEntity(dto.getVitals(), vitals);
    }
  }

  @Transactional
  public AppointmentDTO completeConsultation(UUID id, AppointmentDTO request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Appointment appointment =
        appointmentRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));

    if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
      throw new ApiException(
          "CONSULTATION_ALREADY_COMPLETED",
          "Completed consultations cannot be completed or restarted again",
          HttpStatus.CONFLICT);
    }

    if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
      throw new ApiException(
          "CONSULTATION_CANCELLED",
          "Cancelled appointments cannot be completed",
          HttpStatus.BAD_REQUEST);
    }

    if (appointment.getStatus() != AppointmentStatus.CONSULTATION_STARTED
        && appointment.getStatus() != AppointmentStatus.PRESCRIPTION_FINALIZED) {
      throw new ApiException(
          "CONSULTATION_NOT_STARTED",
          "Only an active consultation can be completed",
          HttpStatus.BAD_REQUEST);
    }

    // Auto-create Lab Order if investigations exist in the request payload
    if (request != null
        && request.getPrescription() != null
        && request.getPrescription().getLabItems() != null
        && !request.getPrescription().getLabItems().isEmpty()) {
      java.util.List<UUID> testIds =
          request.getPrescription().getLabItems().stream()
              .map(
                  com.res.hms.clinical.application.prescription.dto.PrescriptionLabItemDTO
                      ::getTestId)
              .collect(java.util.stream.Collectors.toList());

      LabOrderCreateRequest labRequest = new LabOrderCreateRequest();
      labRequest.setPatientId(appointment.getPatientId());
      labRequest.setEncounterId(
          appointment.getVisitId() != null ? appointment.getVisitId() : appointment.getId());
      labRequest.setTestIds(testIds);

      labOrderService.createOrder(labRequest);
    }

    // Explicitly save/update prescription data before completing
    if (request != null && request.getPrescription() != null) {
      prescriptionService.saveOrUpdatePrescription(appointment, request.getPrescription());
    }

    appointment.setStatus(AppointmentStatus.COMPLETED);
    return appointmentService.toDto(appointmentRepository.saveAndFlush(appointment));
  }

  private void ensureNoOtherConsultationRunning(Appointment appointment) {
    ZoneId zoneId = validationHelper.getHospitalZoneId(appointment.getHospitalId());
    LocalDate appointmentDate = appointment.getAppointmentTime().atZone(zoneId).toLocalDate();
    Instant[] range = validationHelper.getDayRange(appointmentDate, zoneId);

    java.util.List<Appointment> runningAppointments =
        appointmentRepository.findRunningConsultationsForUpdateByDateRange(
            appointment.getHospitalId(), appointment.getDoctorId(), range[0], range[1]);

    boolean anotherConsultationRunning =
        runningAppointments.stream()
            .anyMatch(running -> !running.getId().equals(appointment.getId()));

    if (anotherConsultationRunning) {
      throw new ApiException(
          "CONSULTATION_RUNNING", "One consultation is already in progress", HttpStatus.CONFLICT);
    }
  }
}
