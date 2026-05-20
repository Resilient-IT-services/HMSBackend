package com.res.hms.iam.application.service;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.DoctorAvailabilityException;
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
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final HospitalRepository hospitalRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  private final UserContractRepository assignmentRepository;
  private final com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository
      scheduleRepository;
  private final com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityExceptionRepository
      availabilityExceptionRepository;
  private final com.res.hms.clinical.infrastructure.repository.AppointmentRepository
      appointmentRepository;
  private final com.res.hms.finance.infrastructure.repository.InvoiceItemRepository
      invoiceItemRepository;
  private final com.res.hms.iam.infrastructure.repository.SpecializationRepository
      specializationRepository;

  @Transactional(readOnly = true)
  public com.res.hms.iam.application.dto.DoctorRevenueSummaryDTO getRevenueSummary(
      UUID doctorId, java.time.Instant from, java.time.Instant to) {
    Object[] result = (Object[]) invoiceItemRepository.getDoctorRevenueSummary(doctorId, from, to);

    long count = result != null && result[0] != null ? (long) result[0] : 0;
    java.math.BigDecimal total =
        result != null && result[1] != null
            ? (java.math.BigDecimal) result[1]
            : java.math.BigDecimal.ZERO;
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
    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            doctorProfile.getId(), hospitalId, date);

    if (exceptions.stream().anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
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
    Optional<DoctorAvailabilityException> override =
        exceptions.stream()
            .filter(
                e ->
                    ExceptionType.MODIFIED.equals(e.getType())
                        || ExceptionType.ADDED.equals(e.getType()))
            .findFirst();

    if (override.isPresent()) {
      addOverrideSession(sessions, override.get(), appointments, date, today, now, zoneId);
    } else {
      addBaseSessions(sessions, assignment, date, today, now, appointments, zoneId);
    }

    return new DoctorAvailabilityResponse(sessions);
  }

  private void addOverrideSession(
      List<DoctorAvailabilityResponse.SessionInfo> sessions,
      DoctorAvailabilityException o,
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
    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            doctorProfile.getId(), hospitalId, date);

    if (exceptions.stream().anyMatch(e -> ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
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
    Optional<DoctorAvailabilityException> override =
        exceptions.stream()
            .filter(
                e ->
                    ExceptionType.MODIFIED.equals(e.getType())
                        || ExceptionType.ADDED.equals(e.getType()))
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
    return availabilityExceptionRepository
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

    if (schedules != null) {
      updateSchedules(assignment, schedules);
    }

    if (exceptions != null && assignment.getUser().getDoctorProfile() != null) {
      updateExceptions(assignment, exceptions);
    }

    if (d != null) {
      updateDoctorHospitalDetails(assignment, d);
    }
  }

  private void updateSchedules(UserContract assignment, List<ScheduleDTO> schedules) {
    List<DoctorSchedule> existingSchedules =
        scheduleRepository.findByAssignmentId(assignment.getId());
    com.res.hms.core.util.CollectionMergeUtil.merge(
        existingSchedules,
        schedules,
        DoctorSchedule::getId,
        ScheduleDTO::getId,
        dto -> {
          DoctorSchedule s = new DoctorSchedule();
          s.setAssignmentId(assignment.getId());
          applyScheduleDto(s, dto);
          return s;
        },
        (existing, dto) -> applyScheduleDto(existing, dto),
        s -> {
          scheduleRepository.delete(s);
          existingSchedules.remove(s);
        });
    scheduleRepository.saveAll(existingSchedules.stream().filter(Objects::nonNull).toList());
  }

  private void applyScheduleDto(DoctorSchedule s, ScheduleDTO dto) {
    s.setDayOfWeek(dto.getDayOfWeek());
    s.setStartTime(dto.getStartTime());
    s.setEndTime(dto.getEndTime());
    s.setSlotDurationMinutes(dto.getSlotDurationMinutes());
    s.setMaxPatients(dto.getMaxPatients());
  }

  private void updateExceptions(
      UserContract assignment, List<AvailabilityExceptionDTO> exceptions) {
    DoctorProfile doctorProfile = assignment.getUser().getDoctorProfile();
    List<DoctorAvailabilityException> existingExceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalId(
            doctorProfile.getId(), assignment.getHospitalId());

    com.res.hms.core.util.CollectionMergeUtil.merge(
        existingExceptions,
        exceptions,
        DoctorAvailabilityException::getId,
        AvailabilityExceptionDTO::getId,
        dto -> {
          DoctorAvailabilityException ex = new DoctorAvailabilityException();
          ex.setDoctorId(doctorProfile.getId());
          ex.setHospitalId(assignment.getHospitalId());
          applyExceptionDto(ex, dto);
          return ex;
        },
        (existing, dto) -> applyExceptionDto(existing, dto),
        ex -> {
          availabilityExceptionRepository.delete(ex);
          existingExceptions.remove(ex);
        });
    availabilityExceptionRepository.saveAll(
        existingExceptions.stream().filter(Objects::nonNull).toList());
  }

  private void applyExceptionDto(DoctorAvailabilityException ex, AvailabilityExceptionDTO dto) {
    ex.setDate(dto.getDate());
    ex.setType(dto.getType());
    ex.setStartTime(dto.getStartTime());
    ex.setEndTime(dto.getEndTime());
    ex.setMode(dto.getMode());
    ex.setSlotDurationMinutes(dto.getSlotDurationMinutes());
    ex.setNote(dto.getNote());
  }

  private void updateDoctorHospitalDetails(UserContract assignment, DoctorHospitalDetailsDTO d) {
    DoctorHospitalDetails det =
        doctorHospitalDetailsRepository
            .findById(assignment.getId())
            .orElseGet(
                () -> {
                  DoctorHospitalDetails newDet = new DoctorHospitalDetails();
                  newDet.setAssignment(assignment);
                  return newDet;
                });
    if (d.getConsultationFee() != null) {
      det.setConsultationFee(d.getConsultationFee());
    }
    if (d.getFollowUpDays() != null) {
      det.setFollowUpDays(d.getFollowUpDays());
    }
    if (d.getCommissionType() != null) {
      det.setCommissionType(d.getCommissionType());
    }
    if (d.getCommissionValue() != null) {
      det.setCommissionValue(d.getCommissionValue());
    }
    det.setVisitingConsultant(d.isVisitingConsultant());
    doctorHospitalDetailsRepository.save(det);
    assignment.setDoctorDetails(det);
  }

  @Transactional
  public void deleteHospitalSpecificData(UserContract assignment) {
    scheduleRepository.deleteByAssignmentId(assignment.getId());
    if (assignment.getUser().getDoctorProfile() != null) {
      availabilityExceptionRepository.deleteByDoctorIdAndHospitalId(
          assignment.getUser().getDoctorProfile().getId(), assignment.getHospitalId());
    }
  }

  public DoctorProfile requireDoctor(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, doctorProfileRepository::findByIdAndHospitalId, "DoctorProfile");
  }

  @Transactional(readOnly = true)
  public List<ScheduleDTO> getScheduleDTOs(UUID assignmentId) {
    return scheduleRepository.findByAssignmentId(assignmentId).stream()
        .map(
            s ->
                ScheduleDTO.builder()
                    .id(s.getId())
                    .dayOfWeek(s.getDayOfWeek())
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .slotDurationMinutes(s.getSlotDurationMinutes())
                    .maxPatients(s.getMaxPatients())
                    .build())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AvailabilityExceptionDTO> getExceptionDTOs(UUID userId, UUID hospitalId) {
    DoctorProfile profile = doctorProfileRepository.findByUserId(userId).orElse(null);
    if (profile == null) {
      return Collections.emptyList();
    }
    return availabilityExceptionRepository
        .findByDoctorIdAndHospitalId(profile.getId(), hospitalId)
        .stream()
        .map(
            e ->
                AvailabilityExceptionDTO.builder()
                    .id(e.getId())
                    .date(e.getDate())
                    .type(e.getType())
                    .startTime(e.getStartTime())
                    .endTime(e.getEndTime())
                    .mode(e.getMode())
                    .slotDurationMinutes(e.getSlotDurationMinutes())
                    .note(e.getNote())
                    .build())
        .toList();
  }
}
