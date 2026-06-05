package com.res.hms.clinical.application.appointment.service;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.clinical.domain.Vitals;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
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

  private final HospitalRepository hospitalRepository;
  private final AppointmentRepository appointmentRepository;
  private final DoctorScheduleRepository doctorScheduleRepository;
  private final UserContractRepository userContractRepository;

  public ZoneId getHospitalZoneId(UUID hospitalId) {
    String tz =
        hospitalRepository
            .findById(hospitalId)
            .map(com.res.hms.hospital.domain.Hospital::getTimezone)
            .orElse("Asia/Kolkata");
    return ZoneId.of(tz);
  }

  public Instant[] getDayRange(LocalDate date, ZoneId zoneId) {
    ZonedDateTime startOfDay = date.atStartOfDay(zoneId);
    ZonedDateTime endOfDay = date.plusDays(1).atStartOfDay(zoneId);
    return new Instant[] {startOfDay.toInstant(), endOfDay.toInstant()};
  }

  public void validateAppointmentProximity(
      AppointmentDTO dto, UUID hospitalId, ZoneId zoneId, Instant[] range) {
    UserContract assignment =
        userContractRepository
            .findByDoctorIdAndHospitalId(dto.getDoctorId(), hospitalId)
            .orElse(null);

    if (assignment == null) {
      return;
    }

    LocalDate date = dto.getAppointmentTime().atZoneSameInstant(zoneId).toLocalDate();
    List<DoctorSchedule> schedules =
        doctorScheduleRepository.findByAssignmentAndDay(assignment.getId(), date.getDayOfWeek());

    long bufferSeconds = 60; // Fallback
    if (!schedules.isEmpty()) {
      DoctorSchedule schedule = schedules.get(0);
      if (schedule.getMaxPatients() != null && schedule.getMaxPatients() > 0) {
        long totalMinutes =
            Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
        long avgSlotMinutes = totalMinutes / schedule.getMaxPatients();
        bufferSeconds = (avgSlotMinutes * 60) / 4; // 25% buffer
      }
    }

    Instant requested = dto.getAppointmentTime().toInstant();
    List<Appointment> existing =
        appointmentRepository.findByDoctorIdAndDateRange(dto.getDoctorId(), range[0], range[1]);

    for (Appointment appt : existing) {
      if (dto.getId() != null && appt.getId().equals(dto.getId())) {
        continue;
      }

      long diff = Math.abs(Duration.between(requested, appt.getAppointmentTime()).toSeconds());
      if (diff < bufferSeconds) {
        throw new ApiException(
            "APPOINTMENT_TOO_CLOSE",
            "This appointment is too close to another scheduled appointment. Please choose a different time.",
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  public void mapVitalsDtoToEntity(VitalsDTO dto, Vitals vitals) {
    if (dto == null || vitals == null) {
      return;
    }
    vitals.setBloodPressure(dto.getBloodPressure());
    vitals.setSystolic(dto.getSystolic());
    vitals.setDiastolic(dto.getDiastolic());
    vitals.setHeartRate(dto.getHeartRate());
    vitals.setOxygenLevel(dto.getOxygenLevel());
    vitals.setTemperature(dto.getTemperature());
    vitals.setWeight(dto.getWeight());
  }

  public Integer generateNextToken(UUID doctorId, Instant instant, ZoneId zoneId) {
    LocalDate date = instant.atZone(zoneId).toLocalDate();
    Instant[] range = getDayRange(date, zoneId);
    Integer maxToken =
        appointmentRepository.findMaxTokenByDoctorAndDateRange(doctorId, range[0], range[1]);
    return (maxToken == null) ? 1 : maxToken + 1;
  }
}
