package com.res.hms.clinical.application.appointment.service;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.Vitals;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.service.DoctorProfileService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentValidationHelper {

  private final AppointmentRepository appointmentRepository;
  private final DoctorProfileService doctorProfileService;
  private final HospitalRepository hospitalRepository;

  public void validateAppointmentProximity(
      AppointmentDTO dto, UUID hospitalId, ZoneId zoneId, Instant[] dayRange) {
    if (dto.getDoctorId() == null || dto.getAppointmentTime() == null) {
      return;
    }

    ZonedDateTime appointmentZdt = dto.getAppointmentTime().atZoneSameInstant(zoneId);
    LocalDate appointmentDate = appointmentZdt.toLocalDate();
    java.time.LocalTime requestedTime = appointmentZdt.toLocalTime();

    DoctorAvailabilityResponse availability =
        doctorProfileService.getAvailableSlots(dto.getDoctorId(), appointmentDate);

    if (availability.getSessions() == null || availability.getSessions().isEmpty()) {
      return;
    }

    availability.getSessions().stream()
        .filter(
            s ->
                !requestedTime.isBefore(s.getStartTime()) && requestedTime.isBefore(s.getEndTime()))
        .findFirst()
        .ifPresent(
            session -> {
              Integer max = session.getMaxPatients();
              long bufferSeconds = calculateBuffer(session, max);

              Instant requestedInstant = dto.getAppointmentTime().toInstant();
              Instant startBuffer = requestedInstant.minusSeconds(bufferSeconds);
              Instant endBuffer = requestedInstant.plusSeconds(bufferSeconds);

              List<Appointment> existingAppointments =
                  appointmentRepository.findByDoctorIdAndDateRange(
                      dto.getDoctorId(), dayRange[0], dayRange[1]);

              for (Appointment existing : existingAppointments) {
                if (dto.getId() != null && existing.getId().equals(dto.getId())) {
                  continue;
                }

                Instant existingInstant = existing.getAppointmentTime();
                if (!existingInstant.isBefore(startBuffer) && !existingInstant.isAfter(endBuffer)) {
                  throwProximityException(session, max);
                }
              }
            });
  }

  private long calculateBuffer(
      DoctorAvailabilityResponse.SessionInfo session, Integer max) {
    if (max == null || max <= 0) {
      return 60;
    }
    long sessionMins =
        Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
    double avgSlotMins = (double) sessionMins / max;
    long buffer = (long) (avgSlotMins * 0.25 * 60);
    return buffer <= 0 ? 1 : buffer;
  }

  private void throwProximityException(
      DoctorAvailabilityResponse.SessionInfo session, Integer max) {
    throw new ApiException(
        "APPOINTMENT_TOO_CLOSE",
        "Another appointment is already booked too close to this time. "
            + (max == null || max <= 0
                ? "A default 60-second buffer is required."
                : "For a doctor with "
                    + max
                    + " patients per "
                    + Duration.between(session.getStartTime(), session.getEndTime())
                        .toMinutes()
                    + " minute session, a buffer is required."),
        HttpStatus.BAD_REQUEST);
  }

  public void mapVitalsDtoToEntity(VitalsDTO dto, Vitals entity) {
    if (dto == null || entity == null) {
      return;
    }
    entity.setBloodPressure(dto.getBloodPressure());
    entity.setSystolic(dto.getSystolic());
    entity.setDiastolic(dto.getDiastolic());
    entity.setHeartRate(dto.getHeartRate());
    entity.setOxygenLevel(dto.getOxygenLevel());
    entity.setTemperature(dto.getTemperature());
    entity.setWeight(dto.getWeight());
  }

  public ZoneId getHospitalZoneId(UUID hospitalId) {
    String tz =
        hospitalRepository
            .findById(hospitalId)
            .map(com.res.hms.hospital.domain.Hospital::getTimezone)
            .orElse("Asia/Kolkata");
    return ZoneId.of(tz);
  }

  public Instant[] getDayRange(LocalDate date, ZoneId zoneId) {
    Instant start = date.atStartOfDay(zoneId).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
    return new Instant[] {start, end};
  }

  public Integer generateNextToken(UUID doctorId, Instant appointmentTime, ZoneId zoneId) {
    LocalDate date = appointmentTime.atZone(zoneId).toLocalDate();
    Instant[] range = getDayRange(date, zoneId);
    Integer lastToken =
        appointmentRepository.findMaxTokenByDoctorAndDateRange(doctorId, range[0], range[1]);
    return lastToken == null ? 1 : lastToken + 1;
  }
}
