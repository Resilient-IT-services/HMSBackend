package com.res.hms.iam.application.service;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.DoctorAvailabilityOverride;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.clinical.domain.ExceptionType;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.AvailabilityExceptionDTO;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.dto.DoctorHospitalDetailsDTO;
import com.res.hms.iam.application.dto.DoctorSlotDTO;
import com.res.hms.iam.application.dto.ScheduleDTO;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorProfileService {

  private final DoctorProfileRepository doctorProfileRepository;
  private final DoctorHospitalProfileService doctorHospitalProfileService;
  private final HospitalRepository hospitalRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  private final UserContractRepository assignmentRepository;
  private final com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository
      scheduleRepository;
  private final com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityOverrideRepository
      availabilityOverrideRepository;
  private final com.res.hms.clinical.infrastructure.repository.AppointmentRepository
      appointmentRepository;
  private final com.res.hms.finance.infrastructure.repository.InvoiceItemRepository
      invoiceItemRepository;
  private final com.res.hms.iam.infrastructure.repository.SpecializationRepository
      specializationRepository;

  @Transactional(readOnly = true)
  public com.res.hms.iam.application.dto.DoctorRevenueSummaryDTO getRevenueSummary(
      UUID doctorId, java.time.Instant from, java.time.Instant to) {
    List<Object[]> results = invoiceItemRepository.getDoctorRevenueSummary(doctorId, from, to);
    Object[] result = results != null && !results.isEmpty() ? results.get(0) : null;

    long count = 0;
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;

    if (result != null) {
      if (result[0] instanceof Number) {
        count = ((Number) result[0]).longValue();
      }
      if (result[1] instanceof java.math.BigDecimal) {
        total = (java.math.BigDecimal) result[1];
      }
    }

    java.math.BigDecimal average =
        count > 0
            ? total.divide(java.math.BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
            : java.math.BigDecimal.ZERO;

    return com.res.hms.iam.application.dto.DoctorRevenueSummaryDTO.builder()
        .totalAppointments(count)
        .totalRevenue(total)
        .averageFee(average)
        .build();
  }

  @Transactional(readOnly = true)
  public DoctorAvailabilityResponse getAvailableSlots(UUID doctorId, LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    DoctorProfile doctorProfile = findDoctorProfile(doctorId);

    if (!doctorProfile.getUser().isActive()) {
      throw new com.res.hms.core.exception.ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Doctor is inactive",
          org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    UserContract assignment = findAssignment(doctorProfile, hospitalId);
    List<DoctorAvailabilityOverride> overrides =
        availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(
            doctorProfile.getId(), hospitalId, date);

    if (overrides.stream().anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
      return new DoctorAvailabilityResponse(Collections.emptyList());
    }

    List<Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(doctorProfile.getId(), date);
    ZoneId zoneId = getHospitalZoneId(hospitalId);
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
      addOverrideSession(sessions, availabilityOverride.get(), appointments, date, today, now, zoneId);
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
    // Midnight (00:00) as end time means end of day, always future unless it's midnight
    if (endTime.equals(LocalTime.MIDNIGHT)) {
      return true;
    }
    return endTime.isAfter(now);
  }

  @Transactional(readOnly = true)
  public List<DoctorSlotDTO> getSlots(UUID doctorId, LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    DoctorProfile doctorProfile = findDoctorProfile(doctorId);

    if (!doctorProfile.getUser().isActive()) {
      throw new com.res.hms.core.exception.ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Doctor is inactive",
          org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    UserContract assignment = findAssignment(doctorProfile, hospitalId);
    List<DoctorAvailabilityOverride> overrides =
        availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(
            doctorProfile.getId(), hospitalId, date);

    if (overrides.stream().anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
      return Collections.emptyList();
    }

    List<Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(doctorProfile.getId(), date);
    ZoneId zoneId = getHospitalZoneId(hospitalId);
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

    String status = "AVAILABLE";
    if (isPast) {
      status = "SESSION_EXPIRED";
    } else if (isBooked) {
      status = "BOOKED";
    }

    allSlots.add(
        DoctorSlotDTO.builder()
            .startTime(current)
            .endTime(slotEnd)
            .available(!isPast && !isBooked)
            .status(status)
            .build());
  }

  private DoctorProfile findDoctorProfile(UUID id) {
    return doctorProfileRepository
        .findById(id)
        .or(() -> doctorProfileRepository.findByUserId(id))
        .or(() -> assignmentRepository.findById(id).map(a -> a.getUser().getDoctorProfile()))
        .orElseThrow(
            () ->
                new com.res.hms.core.exception.ApiException(
                    "NOT_FOUND",
                    "DoctorProfile not found",
                    org.springframework.http.HttpStatus.NOT_FOUND));
  }

  private UserContract findAssignment(DoctorProfile profile, UUID hospitalId) {
    return profile.getUser().getContracts().stream()
        .filter(
            a -> hospitalId.equals(a.getHospitalId()) && a.getRole().getCode() == RoleCode.DOCTOR)
        .findFirst()
        .orElseThrow(
            () ->
                new com.res.hms.core.exception.ApiException(
                    "NOT_FOUND",
                    "DoctorProfile assignment not found for this hospital",
                    org.springframework.http.HttpStatus.NOT_FOUND));
  }

  private ZoneId getHospitalZoneId(UUID hospitalId) {
    String tz =
        hospitalRepository
            .findById(hospitalId)
            .map(com.res.hms.hospital.domain.Hospital::getTimezone)
            .orElse("Asia/Kolkata");
    return ZoneId.of(tz);
  }

  @Transactional(readOnly = true)
  public boolean isFullDayOff(UUID doctorId, LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return availabilityOverrideRepository
        .findByDoctorIdAndHospitalIdAndDate(doctorId, hospitalId, date)
        .stream()
        .anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()));
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
    applyProfileUpdates(doctorProfile, info);
    doctorProfileRepository.save(doctorProfile);
    user.setDoctorProfile(doctorProfile);
  }

  private void applyProfileUpdates(DoctorProfile doctorProfile, UserDetailsDTO.DoctorInfo info) {
    if (info.getSpecializations() != null) {
      doctorProfile.setSpecializations(
          info.getSpecializations().stream()
              .map(
                  name ->
                      specializationRepository
                          .findByName(name)
                          .orElseThrow(
                              () ->
                                  new com.res.hms.core.exception.ApiException(
                                      "NOT_FOUND",
                                      "Specialization " + name + " not found in catalog",
                                      org.springframework.http.HttpStatus.BAD_REQUEST)))
              .collect(Collectors.toSet()));
    }
    if (info.getBio() != null) {
      doctorProfile.setBio(info.getBio());
    }
    if (info.getLicenseNumber() != null) {
      doctorProfile.setLicenseNumber(normalizeLicenseNumber(info.getLicenseNumber()));
    }
    if (info.getRegistrationNumber() != null) {
      doctorProfile.setRegistrationNumber(info.getRegistrationNumber());
    }
    if (info.getExperienceYears() != null) {
      doctorProfile.setExperienceYears(info.getExperienceYears());
    }
    if (info.getEducation() != null) {
      doctorProfile.setEducation(info.getEducation());
    }
    if (info.getAwards() != null) {
      doctorProfile.setAwards(info.getAwards());
    }
    if (info.getLanguages() != null) {
      doctorProfile.setLanguages(info.getLanguages());
    }
    if (info.getSignature() != null) {
      doctorProfile.setSignature(info.getSignature());
    }
    if (info.getConsultationFee() != null) {
      doctorProfile.setConsultationFee(info.getConsultationFee());
    }
    if (info.getMetadata() != null) {
      doctorProfile.setMetadata(info.getMetadata());
    }
  }

  private String normalizeLicenseNumber(String licenseNumber) {
    String trimmed = licenseNumber.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @Transactional
  public void updateHospitalDetails(
      UserContract assignment,
      DoctorHospitalDetailsDTO d,
      List<ScheduleDTO> schedules,
      List<AvailabilityExceptionDTO> exceptions) {
    doctorHospitalProfileService.updateHospitalDetails(assignment, d, schedules, exceptions);
  }

  @Transactional
  public void deleteHospitalSpecificData(UserContract assignment) {
    doctorHospitalProfileService.deleteHospitalSpecificData(assignment);
  }

  public DoctorProfile requireDoctor(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, doctorProfileRepository::findByIdAndHospitalId, "DoctorProfile");
  }

  @Transactional(readOnly = true)
  public List<ScheduleDTO> getScheduleDTOs(UUID assignmentId) {
    return doctorHospitalProfileService.getScheduleDTOs(assignmentId);
  }

  @Transactional(readOnly = true)
  public List<AvailabilityExceptionDTO> getExceptionDTOs(UUID userId, UUID hospitalId) {
    return doctorHospitalProfileService.getExceptionDTOs(userId, hospitalId);
  }
}
