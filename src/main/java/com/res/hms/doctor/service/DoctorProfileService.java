package com.res.hms.doctor.service;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.dto.AvailabilityExceptionDTO;
import com.res.hms.doctor.dto.DoctorAvailabilityResponse;
import com.res.hms.doctor.dto.ScheduleDTO;
import com.res.hms.doctor.repository.DoctorProfileRepository;
import com.res.hms.domain.DoctorAvailabilityException;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.DoctorProfile;
import com.res.hms.domain.DoctorSchedule;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.doctor.repository.DoctorHospitalDetailsRepository;
import com.res.hms.hospital.repository.HospitalRepository;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.user.repository.UserRoleAssignmentRepository;
import com.res.hms.doctor.repository.DoctorScheduleRepository;
import com.res.hms.doctor.repository.DoctorAvailabilityExceptionRepository;

import com.res.hms.doctor.mapper.DoctorMapper;

@Service
@RequiredArgsConstructor
public class DoctorProfileService {

  private final DoctorProfileRepository doctorProfileRepository;
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final HospitalRepository hospitalRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;
  private final DoctorMapper doctorMapper;

  private final UserRoleAssignmentRepository assignmentRepository;
  private final DoctorScheduleRepository scheduleRepository;
  private final DoctorAvailabilityExceptionRepository
      availabilityExceptionRepository;
  private final com.res.hms.appointment.repository.AppointmentRepository appointmentRepository;

  @Transactional(readOnly = true)
  public DoctorAvailabilityResponse getAvailableSlots(UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    short dbDay = (short) date.getDayOfWeek().getValue();

    org.slf4j.LoggerFactory.getLogger(DoctorProfileService.class)
        .info(
            "Checking availability for DoctorProfile: {}, Hospital: {}, Date: {}, Day: {}",
            doctorId,
            hospitalId,
            date,
            dbDay);

    // 1. Find the DoctorProfile (try by doctorId, userId, or assignmentId)
    DoctorProfile doctorProfile =
        doctorProfileRepository
            .findById(doctorId)
            .or(() -> doctorProfileRepository.findByUserId(doctorId))
            .or(
                () ->
                    assignmentRepository
                        .findById(doctorId)
                        .map(a -> a.getUser().getDoctorProfile()))
            .orElseThrow(
                () ->
                    new com.res.hms.common.exception.ApiException(
                        "NOT_FOUND",
                        "DoctorProfile not found",
                        org.springframework.http.HttpStatus.NOT_FOUND));

    UserAccount user = doctorProfile.getUser();
    UUID actualDoctorId = doctorProfile.getId();
    // 2. Find the relevant assignment for this hospital
    UserRoleAssignment assignment =
        user.getAssignments().stream()
            .filter(
                a ->
                    hospitalId.equals(a.getHospitalId())
                        && a.getRole().getCode() == RoleCode.DOCTOR)
            .findFirst()
            .orElseThrow(
                () ->
                    new com.res.hms.common.exception.ApiException(
                        "NOT_FOUND",
                        "DoctorProfile assignment not found for this hospital",
                        org.springframework.http.HttpStatus.NOT_FOUND));

    // 3. Check for exceptions
    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            actualDoctorId, hospitalId, date);

    if (exceptions.stream().anyMatch(e -> "FULL_DAY_OFF".equals(e.getType()))) {
      return new DoctorAvailabilityResponse(java.util.Collections.emptyList());
    }

    // 4. Fetch existing appointments
    java.util.List<com.res.hms.domain.Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(actualDoctorId, date);

    // 5. Determine sessions
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalTime now = java.time.LocalTime.now();

    if (date.isBefore(today)) {
      return new DoctorAvailabilityResponse(java.util.Collections.emptyList());
    }

    List<DoctorAvailabilityResponse.SessionInfo> sessions = new java.util.ArrayList<>();
    Optional<DoctorAvailabilityException> override =
        exceptions.stream()
            .filter(e -> "MODIFIED".equals(e.getType()) || "ADDED".equals(e.getType()))
            .findFirst();

    if (override.isPresent()) {
      var o = override.get();
      if (date.isAfter(today) || o.getEndTime().isAfter(now)) {
        int count = countBookingsInRange(appointments, o.getStartTime(), o.getEndTime());
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
    } else {
      List<DoctorSchedule> baseSchedules =
          assignment.getSchedules().stream().filter(s -> s.getDayOfWeek().equals(dbDay)).toList();

      for (DoctorSchedule s : baseSchedules) {
        if (date.isAfter(today) || s.getEndTime().isAfter(now)) {
          int count = countBookingsInRange(appointments, s.getStartTime(), s.getEndTime());
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

    return new DoctorAvailabilityResponse(sessions);
  }

  @Transactional(readOnly = true)
  public List<com.res.hms.doctor.dto.DoctorSlotDTO> getSlots(
      UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    short dbDay = (short) date.getDayOfWeek().getValue();

    DoctorProfile doctorProfile =
        doctorProfileRepository
            .findById(doctorId)
            .or(() -> doctorProfileRepository.findByUserId(doctorId))
            .orElseThrow(
                () ->
                    new com.res.hms.common.exception.ApiException(
                        "NOT_FOUND",
                        "DoctorProfile not found",
                        org.springframework.http.HttpStatus.NOT_FOUND));

    UUID actualDoctorId = doctorProfile.getId();
    UserAccount user = doctorProfile.getUser();
    UserRoleAssignment assignment =
        user.getAssignments().stream()
            .filter(
                a ->
                    hospitalId.equals(a.getHospitalId())
                        && a.getRole().getCode() == RoleCode.DOCTOR)
            .findFirst()
            .orElseThrow(
                () ->
                    new com.res.hms.common.exception.ApiException(
                        "NOT_FOUND",
                        "DoctorProfile assignment not found for this hospital",
                        org.springframework.http.HttpStatus.NOT_FOUND));

    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            actualDoctorId, hospitalId, date);

    if (exceptions.stream().anyMatch(e -> "FULL_DAY_OFF".equals(e.getType()))) {
      return java.util.Collections.emptyList();
    }

    java.util.List<com.res.hms.domain.Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(actualDoctorId, date);
    java.util.Set<LocalTime> bookedTimes =
        appointments.stream()
            .map(a -> a.getAppointmentTime().toLocalTime())
            .collect(java.util.stream.Collectors.toSet());

    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalTime now = java.time.LocalTime.now();

    List<com.res.hms.doctor.dto.DoctorSlotDTO> allSlots = new java.util.ArrayList<>();
    Optional<DoctorAvailabilityException> override =
        exceptions.stream()
            .filter(e -> "MODIFIED".equals(e.getType()) || "ADDED".equals(e.getType()))
            .findFirst();

    if (override.isPresent()) {
      generateSlots(
          override.get().getStartTime(),
          override.get().getEndTime(),
          override.get().getSlotDurationMinutes() != null
              ? override.get().getSlotDurationMinutes()
              : 15,
          bookedTimes,
          date,
          today,
          now,
          allSlots);
    } else {
      List<DoctorSchedule> baseSchedules =
          assignment.getSchedules().stream().filter(s -> s.getDayOfWeek().equals(dbDay)).toList();
      for (DoctorSchedule s : baseSchedules) {
        generateSlots(
            s.getStartTime(),
            s.getEndTime(),
            s.getSlotDurationMinutes(),
            bookedTimes,
            date,
            today,
            now,
            allSlots);
      }
    }

    return allSlots;
  }

  @Transactional(readOnly = true)
  public boolean isFullDayOff(UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return availabilityExceptionRepository
        .findByDoctorIdAndHospitalIdAndDate(doctorId, hospitalId, date)
        .stream()
        .anyMatch(e -> "FULL_DAY_OFF".equals(e.getType()));
  }

  private void generateSlots(
      LocalTime start,
      LocalTime end,
      int duration,
      java.util.Set<LocalTime> bookedTimes,
      java.time.LocalDate date,
      java.time.LocalDate today,
      LocalTime now,
      List<com.res.hms.doctor.dto.DoctorSlotDTO> allSlots) {
    LocalTime current = start;
    while (current.plusMinutes(duration).isBefore(end)
        || current.plusMinutes(duration).equals(end)) {
      LocalTime slotEnd = current.plusMinutes(duration);
      boolean isPast = date.isBefore(today) || (date.equals(today) && current.isBefore(now));
      boolean isBooked = bookedTimes.contains(current);

      String status = "AVAILABLE";
      if (isPast) {
        status = "SESSION_EXPIRED";
      } else if (isBooked) {
        status = "BOOKED";
      }

      allSlots.add(
          com.res.hms.doctor.dto.DoctorSlotDTO.builder()
              .startTime(current)
              .endTime(slotEnd)
              .available(!isPast && !isBooked)
              .status(status)
              .build());
      current = slotEnd;
    }
  }

  private int countBookingsInRange(
      java.util.List<com.res.hms.domain.Appointment> appointments, LocalTime start, LocalTime end) {
    return (int)
        appointments.stream()
            .map(a -> a.getAppointmentTime().toLocalTime())
            .filter(t -> (t.equals(start) || t.isAfter(start)) && t.isBefore(end))
            .count();
  }

  private LocalTime calculateSuggestedTime(
      LocalTime sessionStart, int currentBookings, int durationMinutes) {
    return sessionStart.plusMinutes((long) currentBookings * durationMinutes);
  }

  @Transactional
  public void updateProfile(UserAccount user, UserDetailsDTO.DoctorInfo info) {
    if (info == null) {
      return;
    }
    DoctorProfile doctorProfile =
        doctorProfileRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  DoctorProfile d = new DoctorProfile();
                  d.setUser(user);
                  return d;
                });

    doctorMapper.updateEntity(info, doctorProfile);
    if (info.getLicenseNumber() != null) {
      doctorProfile.setLicenseNumber(normalizeLicenseNumber(info.getLicenseNumber()));
    }

    doctorProfileRepository.save(doctorProfile);
    user.setDoctorProfile(doctorProfile);
  }

  private String normalizeLicenseNumber(String licenseNumber) {
    String trimmed = licenseNumber.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @Transactional
  public void updateHospitalDetails(
      UserRoleAssignment assignment,
      DoctorHospitalDetailsDTO d,
      List<ScheduleDTO> schedules,
      List<AvailabilityExceptionDTO> exceptions) {

    if (schedules != null) {
      com.res.hms.common.util.CollectionMergeUtil.merge(
          assignment.getSchedules(),
          schedules,
          DoctorSchedule::getId,
          ScheduleDTO::getId,
          dto -> {
            DoctorSchedule s = doctorMapper.toScheduleEntity(dto);
            s.setAssignment(assignment);
            return s;
          },
          (existing, dto) -> doctorMapper.updateEntity(dto, existing),
          assignment.getSchedules()::remove);
    }

    if (exceptions != null && assignment.getUser().getDoctorProfile() != null) {
      DoctorProfile doctorProfile = assignment.getUser().getDoctorProfile();
      Hospital hospital =
          resourceLookup.findRequired(
              assignment.getHospitalId(), hospitalRepository::findById, "Hospital");

      com.res.hms.common.util.CollectionMergeUtil.merge(
          doctorProfile.getAvailabilityExceptions(),
          exceptions,
          DoctorAvailabilityException::getId,
          AvailabilityExceptionDTO::getId,
          dto -> {
            DoctorAvailabilityException ex = doctorMapper.toExceptionEntity(dto);
            ex.setDoctorProfile(doctorProfile);
            ex.setHospital(hospital);
            return ex;
          },
          (existing, dto) -> doctorMapper.updateEntity(dto, existing),
          doctorProfile.getAvailabilityExceptions()::remove);
    }

    if (d == null) {
      return;
    }
    DoctorHospitalDetails det =
        doctorHospitalDetailsRepository
            .findById(assignment.getId())
            .orElseGet(
                () -> {
                  DoctorHospitalDetails newDet = new DoctorHospitalDetails();
                  newDet.setAssignment(assignment);
                  return newDet;
                });

    doctorMapper.updateEntity(d, det);
    doctorHospitalDetailsRepository.save(det);
    assignment.setDoctorDetails(det);
  }

  public DoctorProfile requireDoctor(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, doctorProfileRepository::findByIdAndHospitalId, "DoctorProfile");
  }
}
