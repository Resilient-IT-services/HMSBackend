package com.res.hms.iam.application.service;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.DoctorAvailabilityOverride;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.clinical.domain.ExceptionType;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityOverrideRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.dto.DoctorSlotDTO;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.UserContract;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorAvailabilityService {

  private final DoctorScheduleRepository scheduleRepository;
  private final DoctorAvailabilityOverrideRepository availabilityOverrideRepository;
  private final AppointmentRepository appointmentRepository;

  @Transactional(readOnly = true)
  public DoctorAvailabilityResponse getAvailableSlots(
      DoctorProfile doctorProfile, UserContract assignment, LocalDate date, ZoneId zoneId) {
    List<DoctorAvailabilityOverride> overrides =
        availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(
            doctorProfile.getId(), assignment.getHospitalId(), date);

    if (overrides.stream().anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
      return new DoctorAvailabilityResponse(Collections.emptyList());
    }

    List<Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(doctorProfile.getId(), date);
    ZonedDateTime nowHospital = ZonedDateTime.now(zoneId);
    LocalDate today = nowHospital.toLocalDate();
    LocalTime now = nowHospital.toLocalTime();

    if (date.isBefore(today)) {
      return new DoctorAvailabilityResponse(Collections.emptyList());
    }

    List<DoctorAvailabilityResponse.SessionInfo> sessions = new ArrayList<>();
    Optional<DoctorAvailabilityOverride> availabilityOverride =
        overrides.stream()
            .filter(
                e ->
                    ExceptionType.MODIFIED.equals(e.getType())
                        || ExceptionType.ADDED.equals(e.getType()))
            .findFirst();

    if (availabilityOverride.isPresent()) {
      addOverrideSession(
          sessions, availabilityOverride.get(), appointments, date, today, now, zoneId);
    } else {
      addBaseSessions(sessions, assignment, date, today, now, appointments, zoneId);
    }

    return new DoctorAvailabilityResponse(sessions);
  }

  private void addOverrideSession(
      List<DoctorAvailabilityResponse.SessionInfo> sessions,
      DoctorAvailabilityOverride o,
      List<Appointment> appointments,
      LocalDate date,
      LocalDate today,
      LocalTime now,
      ZoneId zoneId) {
    if (date.isAfter(today) || (o.getEndTime() != null && isTimeFuture(o.getEndTime(), now))) {
      int count = countBookingsInRange(appointments, o.getStartTime(), o.getEndTime(), zoneId);
      int duration = o.getSlotDurationMinutes() != null ? o.getSlotDurationMinutes() : 15;

      sessions.add(
          new DoctorAvailabilityResponse.SessionInfo(
              null,
              o.getStartTime(),
              o.getEndTime(),
              null,
              count,
              true,
              calculateSuggestedTime(o.getStartTime(), count, duration)));
    }
  }

  private void addBaseSessions(
      List<DoctorAvailabilityResponse.SessionInfo> sessions,
      UserContract assignment,
      LocalDate date,
      LocalDate today,
      LocalTime now,
      List<Appointment> appointments,
      ZoneId zoneId) {
    List<DoctorSchedule> baseSchedules =
        scheduleRepository.findByAssignmentId(assignment.getId()).stream()
            .filter(s -> s.getDayOfWeek().equals(date.getDayOfWeek()))
            .toList();

    for (DoctorSchedule s : baseSchedules) {
      if (date.isAfter(today) || (s.getEndTime() != null && isTimeFuture(s.getEndTime(), now))) {
        int count = countBookingsInRange(appointments, s.getStartTime(), s.getEndTime(), zoneId);
        int max = s.getMaxPatients() != null ? s.getMaxPatients() : Integer.MAX_VALUE;
        int duration = s.getSlotDurationMinutes() != null ? s.getSlotDurationMinutes() : 15;

        sessions.add(
            new DoctorAvailabilityResponse.SessionInfo(
                s.getId(),
                s.getStartTime(),
                s.getEndTime(),
                s.getMaxPatients(),
                count,
                count < max,
                calculateSuggestedTime(s.getStartTime(), count, duration)));
      }
    }
  }

  private boolean isTimeFuture(LocalTime endTime, LocalTime now) {
    if (endTime.equals(LocalTime.MIDNIGHT)) {
      return true;
    }
    return endTime.isAfter(now);
  }

  @Transactional(readOnly = true)
  public List<DoctorSlotDTO> getSlots(
      DoctorProfile doctorProfile, UserContract assignment, LocalDate date, ZoneId zoneId) {
    List<DoctorAvailabilityOverride> overrides =
        availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(
            doctorProfile.getId(), assignment.getHospitalId(), date);

    if (overrides.stream().anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
      return Collections.emptyList();
    }

    List<Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(doctorProfile.getId(), date);
    Set<LocalTime> bookedTimes =
        appointments.stream()
            .map(a -> a.getAppointmentTime().atZone(zoneId).toLocalTime())
            .collect(Collectors.toSet());

    ZonedDateTime nowHospital = ZonedDateTime.now(zoneId);
    LocalDate today = nowHospital.toLocalDate();
    LocalTime now = nowHospital.toLocalTime();

    List<DoctorSlotDTO> allSlots = new ArrayList<>();
    Optional<DoctorAvailabilityOverride> availabilityOverride =
        overrides.stream()
            .filter(
                e ->
                    ExceptionType.MODIFIED.equals(e.getType())
                        || ExceptionType.ADDED.equals(e.getType()))
            .findFirst();

    if (availabilityOverride.isPresent()) {
      generateSlots(
          availabilityOverride.get().getStartTime(),
          availabilityOverride.get().getEndTime(),
          availabilityOverride.get().getSlotDurationMinutes() != null
              ? availabilityOverride.get().getSlotDurationMinutes()
              : 15,
          bookedTimes,
          date,
          today,
          now,
          allSlots);
    } else {
      List<DoctorSchedule> baseSchedules =
          scheduleRepository.findByAssignmentId(assignment.getId()).stream()
              .filter(s -> s.getDayOfWeek().equals(date.getDayOfWeek()))
              .toList();
      for (DoctorSchedule s : baseSchedules) {
        generateSlots(
            s.getStartTime(),
            s.getEndTime(),
            s.getSlotDurationMinutes() != null ? s.getSlotDurationMinutes() : 15,
            bookedTimes,
            date,
            today,
            now,
            allSlots);
      }
    }
    return allSlots;
  }

  private void generateSlots(
      LocalTime start,
      LocalTime end,
      int duration,
      Set<LocalTime> bookedTimes,
      LocalDate date,
      LocalDate today,
      LocalTime now,
      List<DoctorSlotDTO> allSlots) {
    LocalTime current = start;
    while (true) {
      LocalTime slotEnd = current.plusMinutes(duration);
      boolean wrapped = slotEnd.isBefore(current);
      if (wrapped && !end.equals(LocalTime.MIDNIGHT)) {
        break;
      }
      if (!wrapped && !end.equals(LocalTime.MIDNIGHT) && slotEnd.isAfter(end)) {
        break;
      }
      if (wrapped && slotEnd.equals(LocalTime.MIDNIGHT) && end.equals(LocalTime.MIDNIGHT)) {
        addSlot(current, slotEnd, bookedTimes, date, today, now, allSlots);
        break;
      }
      if (wrapped && !slotEnd.equals(LocalTime.MIDNIGHT) && end.equals(LocalTime.MIDNIGHT)) {
        break;
      }
      addSlot(current, slotEnd, bookedTimes, date, today, now, allSlots);
      if (slotEnd.equals(end)) {
        break;
      }
      current = slotEnd;
    }
  }

  private void addSlot(
      LocalTime current,
      LocalTime slotEnd,
      Set<LocalTime> bookedTimes,
      LocalDate date,
      LocalDate today,
      LocalTime now,
      List<DoctorSlotDTO> allSlots) {
    boolean isPast = date.isBefore(today) || (date.equals(today) && current.isBefore(now));
    boolean isBooked = bookedTimes.contains(current);
    String status = isPast ? "SESSION_EXPIRED" : (isBooked ? "BOOKED" : "AVAILABLE");

    allSlots.add(
        DoctorSlotDTO.builder()
            .startTime(current)
            .endTime(slotEnd)
            .available(!isPast && !isBooked)
            .status(status)
            .build());
  }

  private int countBookingsInRange(
      List<Appointment> appointments, LocalTime start, LocalTime end, ZoneId zoneId) {
    return (int)
        appointments.stream()
            .map(a -> a.getAppointmentTime().atZone(zoneId).toLocalTime())
            .filter(t -> (t.equals(start) || t.isAfter(start)) && t.isBefore(end))
            .count();
  }

  private LocalTime calculateSuggestedTime(
      LocalTime sessionStart, int currentBookings, int durationMinutes) {
    return sessionStart.plusMinutes((long) currentBookings * durationMinutes);
  }

  @Transactional(readOnly = true)
  public boolean isFullDayOff(UUID doctorId, UUID hospitalId, LocalDate date) {
    return availabilityOverrideRepository
        .findByDoctorIdAndHospitalIdAndDate(doctorId, hospitalId, date)
        .stream()
        .anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()));
  }
}
