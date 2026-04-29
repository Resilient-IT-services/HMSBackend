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
  private final com.res.hms.common.validation.VitalValidationService vitalValidationService;
  private final com.res.hms.billing.service.BillingService billingService;

  @Transactional
  public AppointmentDTO saveAppointmentAggregate(AppointmentDTO dto) {
    log.info("Starting aggregate save for appointment");
    UUID hospitalId = hospitalContext.requireHospitalId();

    if (dto.getVitals() != null) {
      vitalValidationService.validate(dto.getVitals());
    }

    if (dto.getDoctorId() != null && dto.getAppointmentTime() != null) {
      validateSlotAvailability(dto.getDoctorId(), dto.getAppointmentTime(), dto.getId());
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

    appointment = appointmentRepository.saveAndFlush(appointment);

    if (isNew) {
      createInitialBilling(appointment);
    }

    return appointmentService.toDto(appointment);
  }

  private void createInitialBilling(Appointment appointment) {
    // Only create billing if it doesn't exist and status is SCHEDULED or PAYMENT_PENDING
    if (appointment.getStatus() == AppointmentStatus.SCHEDULED || appointment.getStatus() == AppointmentStatus.PAYMENT_PENDING) {
      java.math.BigDecimal fee = appointment.getDoctorProfile().getUser().getAssignments().stream()
          .filter(a -> a.getHospitalId().equals(appointment.getHospitalId()) && a.getRole().getCode() == com.res.hms.domain.RoleCode.DOCTOR)
          .findFirst()
          .map(com.res.hms.domain.UserRoleAssignment::getDoctorDetails)
          .map(com.res.hms.domain.DoctorHospitalDetails::getConsultationFee)
          .orElse(java.math.BigDecimal.valueOf(500));

      com.res.hms.billing.dto.BillingDTO billingDto = com.res.hms.billing.dto.BillingDTO.builder()
          .patientId(appointment.getPatient().getId())
          .appointmentId(appointment.getId())
          .sourceType(com.res.hms.domain.BillingSourceType.APPOINTMENT)
          .status(appointment.getStatus() == AppointmentStatus.PAYMENT_PENDING ? com.res.hms.domain.BillingStatus.PENDING : com.res.hms.domain.BillingStatus.PAID)
          .totalAmount(fee)
          .items(java.util.List.of(
              com.res.hms.billing.dto.BillingItemDTO.builder()
                  .itemName("Consultation Fee")
                  .amount(fee)
                  .quantity(1)
                  .subtotal(fee)
                  .build()
          ))
          .build();
      billingService.createBilling(billingDto);
    }
  }

  private void validateSlotAvailability(UUID doctorId, java.time.LocalDateTime appointmentTime, UUID appointmentId) {
    java.time.LocalDate date = appointmentTime.toLocalDate();
    java.time.LocalTime time = appointmentTime.toLocalTime();
    
    if (doctorProfileService.isFullDayOff(doctorId, date)) {
      throw new ApiException("VALIDATION_ERROR", "Doctor is on full day off for the selected date.", HttpStatus.BAD_REQUEST);
    }

    var slots = doctorProfileService.getSlots(doctorId, date);
    if (slots.isEmpty()) {
      // If no slots are defined at all, we assume no schedule is set up and allow it
      // This maintains backward compatibility and makes tests easier
      return;
    }
    
    boolean validSlot = slots.stream()
        .anyMatch(s -> s.getStartTime().equals(time) && (s.isAvailable() || (appointmentId != null && isAlreadyBookedBySameAppointment(doctorId, appointmentTime, appointmentId))));
    
    if (!validSlot) {
      throw new ApiException("VALIDATION_ERROR", "No available slot found for the selected appointment time.", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean isAlreadyBookedBySameAppointment(UUID doctorId, java.time.LocalDateTime appointmentTime, UUID appointmentId) {
      return appointmentRepository.findById(appointmentId)
          .map(a -> a.getDoctorProfile().getId().equals(doctorId) && a.getAppointmentTime().equals(appointmentTime))
          .orElse(false);
  }

  @Transactional
  public AppointmentDTO updateVitals(UUID id, com.res.hms.appointment.dto.VitalsDTO vitalsDto) {
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
    mapVitalsDtoToEntity(vitalsDto, vitals);

    appointmentRepository.saveAndFlush(appointment);
    return appointmentService.toDto(appointment);
  }

  private void mapVitalsDtoToEntity(com.res.hms.appointment.dto.VitalsDTO dto, com.res.hms.domain.Vitals entity) {
    entity.setHeartRate(dto.getHeartRate());
    entity.setOxygenLevel(dto.getOxygenLevel());
    entity.setTemperature(dto.getTemperature());
    if (dto.getTemperatureUnit() != null) {
      entity.setTemperatureUnit(dto.getTemperatureUnit());
    }
    entity.setWeight(dto.getWeight());
    if (dto.getWeightUnit() != null) {
      entity.setWeightUnit(dto.getWeightUnit());
    }

    // Sync BP fields
    if (dto.getSystolic() != null && dto.getDiastolic() != null) {
      entity.setSystolic(dto.getSystolic());
      entity.setDiastolic(dto.getDiastolic());
      entity.setBloodPressure(dto.getSystolic() + "/" + dto.getDiastolic());
    } else if (dto.getBloodPressure() != null && dto.getBloodPressure().matches("^\\d{2,3}/\\d{2,3}$")) {
      String[] parts = dto.getBloodPressure().split("/");
      entity.setBloodPressure(dto.getBloodPressure());
      entity.setSystolic(Integer.parseInt(parts[0]));
      entity.setDiastolic(Integer.parseInt(parts[1]));
    } else {
      entity.setBloodPressure(dto.getBloodPressure());
      entity.setSystolic(dto.getSystolic());
      entity.setDiastolic(dto.getDiastolic());
    }
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
    if (dto.getAppointmentTime() != null) {
      appointment.setSessionPeriod(determineSessionPeriod(dto.getAppointmentTime()));
    }
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
    if (dto.getPatientId() != null
        && !appointment.getPatient().getId().equals(dto.getPatientId())) {
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

    if (dto.getAppointmentTime() != null) {
      appointment.setSessionPeriod(determineSessionPeriod(dto.getAppointmentTime()));
    }

    updateAppointmentFields(appointment, dto);
  }

  private com.res.hms.domain.SessionPeriod determineSessionPeriod(
      java.time.LocalDateTime time) {
    int hour = time.getHour();
    if (hour >= 6 && hour < 12) {
      return com.res.hms.domain.SessionPeriod.MORNING;
    } else if (hour >= 12 && hour < 17) {
      return com.res.hms.domain.SessionPeriod.AFTERNOON;
    } else {
      return com.res.hms.domain.SessionPeriod.EVENING;
    }
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
      mapVitalsDtoToEntity(dto.getVitals(), vitals);
    }
  }
}
