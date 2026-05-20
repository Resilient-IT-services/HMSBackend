package com.res.hms.iam.application.service;

import com.res.hms.clinical.domain.DoctorAvailabilityException;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.AvailabilityExceptionDTO;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.dto.DoctorHospitalDetailsDTO;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final com.res.hms.finance.infrastructure.repository.InvoiceItemRepository invoiceItemRepository;

  @Transactional(readOnly = true)
  public com.res.hms.iam.application.dto.DoctorRevenueSummaryDTO getRevenueSummary(
      UUID doctorId, java.time.Instant from, java.time.Instant to) {
    Object[] result = (Object[]) invoiceItemRepository.getDoctorRevenueSummary(doctorId, from, to);
    
    long count = result != null && result[0] != null ? (long) result[0] : 0;
    java.math.BigDecimal total = result != null && result[1] != null ? (java.math.BigDecimal) result[1] : java.math.BigDecimal.ZERO;
    java.math.BigDecimal average = count > 0 ? total.divide(java.math.BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO;

    return com.res.hms.iam.application.dto.DoctorRevenueSummaryDTO.builder()
        .totalAppointments(count)
        .totalRevenue(total)
        .averageFee(average)
        .build();
  }

  @Transactional(readOnly = true)
  public DoctorAvailabilityResponse getAvailableSlots(UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    java.time.DayOfWeek dbDay = date.getDayOfWeek();

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DoctorProfileService.class);
    log.info(
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
                () -> {
                  log.warn("DoctorProfile not found for ID: {}", doctorId);
                  return new com.res.hms.core.exception.ApiException(
                      "NOT_FOUND",
                      "DoctorProfile not found",
                      org.springframework.http.HttpStatus.NOT_FOUND);
                });

    if (!doctorProfile.getUser().isActive()) {
      log.warn("Doctor user {} is inactive", doctorProfile.getUser().getUsername());
      throw new com.res.hms.core.exception.ApiException(
          "BUSINESS_RULE_VIOLATION", "Doctor is inactive", org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    UserAccount user = doctorProfile.getUser();
    UUID actualDoctorId = doctorProfile.getId();
    log.info("Found actualDoctorId: {} for user: {}", actualDoctorId, user.getUsername());

    // 2. Find the relevant assignment for this hospital
    UserContract assignment =
        user.getContracts().stream()
            .filter(
                a ->
                    hospitalId.equals(a.getHospitalId())
                        && a.getRole().getCode() == RoleCode.DOCTOR)
            .findFirst()
            .orElseThrow(
                () -> {
                  log.warn("Doctor {} has no assignment for hospital {}", user.getUsername(), hospitalId);
                  return new com.res.hms.core.exception.ApiException(
                      "NOT_FOUND",
                      "DoctorProfile assignment not found for this hospital",
                      org.springframework.http.HttpStatus.NOT_FOUND);
                });

    log.info("Found assignmentId: {}", assignment.getId());

    // 3. Check for exceptions
    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            actualDoctorId, hospitalId, date);

    if (exceptions.stream().anyMatch(e -> com.res.hms.clinical.domain.ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
      log.info("Doctor has FULL_DAY_OFF exception for date {}", date);
      return new DoctorAvailabilityResponse(java.util.Collections.emptyList());
    }

    // 4. Fetch existing appointments
    java.util.List<com.res.hms.clinical.domain.Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(actualDoctorId, date);
    log.info("Found {} existing appointments for date {}", appointments.size(), date);

    // 5. Determine sessions
    String tz = hospitalRepository.findById(hospitalId)
        .map(com.res.hms.hospital.domain.Hospital::getTimezone)
        .orElse("Asia/Kolkata");
    java.time.ZoneId zoneId = java.time.ZoneId.of(tz);
    java.time.ZonedDateTime nowHospital = java.time.ZonedDateTime.now(zoneId);
    java.time.LocalDate today = nowHospital.toLocalDate();
    java.time.LocalTime now = nowHospital.toLocalTime();

    log.info("Hospital Timezone: {}, Current Hospital Time: {}, Today: {}, Now: {}", tz, nowHospital, today, now);

    if (date.isBefore(today)) {
      log.info("Requested date {} is before today {}", date, today);
      return new DoctorAvailabilityResponse(java.util.Collections.emptyList());
    }

    List<DoctorAvailabilityResponse.SessionInfo> sessions = new java.util.ArrayList<>();
    Optional<DoctorAvailabilityException> override =
        exceptions.stream()
            .filter(
                e ->
                    com.res.hms.clinical.domain.ExceptionType.MODIFIED.equals(e.getType())
                        || com.res.hms.clinical.domain.ExceptionType.ADDED.equals(e.getType()))
            .findFirst();

    if (override.isPresent()) {
      var o = override.get();
      log.info("Found override exception: {} ({} - {})", o.getType(), o.getStartTime(), o.getEndTime());
      if (date.isAfter(today) || (o.getEndTime() != null && o.getEndTime().isAfter(now))) {
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
      } else {
        log.info("Override exception has already passed for today");
      }
    } else {
      List<DoctorSchedule> baseSchedules =
          scheduleRepository.findByAssignmentId(assignment.getId()).stream()
              .filter(s -> s.getDayOfWeek().equals(dbDay))
              .toList();

      log.info("Found {} base schedules for day {}", baseSchedules.size(), dbDay);

      for (DoctorSchedule s : baseSchedules) {
        log.info("Checking schedule session: {} - {}", s.getStartTime(), s.getEndTime());
        if (date.isAfter(today) || (s.getEndTime() != null && s.getEndTime().isAfter(now))) {
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
        } else {
          log.info("Schedule session has already passed for today (End: {}, Now: {})", s.getEndTime(), now);
        }
      }
    }

    log.info("Returning {} sessions", sessions.size());
    return new DoctorAvailabilityResponse(sessions);
  }

  @Transactional(readOnly = true)
  public List<com.res.hms.iam.application.dto.DoctorSlotDTO> getSlots(
      UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    java.time.DayOfWeek dbDay = date.getDayOfWeek();

    DoctorProfile doctorProfile =
        doctorProfileRepository
            .findById(doctorId)
            .or(() -> doctorProfileRepository.findByUserId(doctorId))
            .orElseThrow(
                () ->
                    new com.res.hms.core.exception.ApiException(
                        "NOT_FOUND",
                        "DoctorProfile not found",
                        org.springframework.http.HttpStatus.NOT_FOUND));

    if (!doctorProfile.getUser().isActive()) {
      throw new com.res.hms.core.exception.ApiException(
          "BUSINESS_RULE_VIOLATION", "Doctor is inactive", org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    UUID actualDoctorId = doctorProfile.getId();
    UserAccount user = doctorProfile.getUser();
    UserContract assignment =
        user.getContracts().stream()
            .filter(
                a ->
                    hospitalId.equals(a.getHospitalId())
                        && a.getRole().getCode() == RoleCode.DOCTOR)
            .findFirst()
            .orElseThrow(
                () ->
                    new com.res.hms.core.exception.ApiException(
                        "NOT_FOUND",
                        "DoctorProfile assignment not found for this hospital",
                        org.springframework.http.HttpStatus.NOT_FOUND));

    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            actualDoctorId, hospitalId, date);

    if (exceptions.stream().anyMatch(e -> com.res.hms.clinical.domain.ExceptionType.FULL_DAY_OFF.equals(e.getType()))) {
      return java.util.Collections.emptyList();
    }

    java.util.List<com.res.hms.clinical.domain.Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(actualDoctorId, date);
    
    String tz = hospitalRepository.findById(hospitalId)
        .map(com.res.hms.hospital.domain.Hospital::getTimezone)
        .orElse("Asia/Kolkata");
    java.time.ZoneId zoneId = java.time.ZoneId.of(tz);

    java.util.Set<LocalTime> bookedTimes =
        appointments.stream()
            .map(a -> a.getAppointmentTime().atZone(zoneId).toLocalTime())
            .collect(java.util.stream.Collectors.toSet());

    java.time.ZonedDateTime nowHospital = java.time.ZonedDateTime.now(zoneId);
    java.time.LocalDate today = nowHospital.toLocalDate();
    java.time.LocalTime now = nowHospital.toLocalTime();

    List<com.res.hms.iam.application.dto.DoctorSlotDTO> allSlots = new java.util.ArrayList<>();
    Optional<DoctorAvailabilityException> override =
        exceptions.stream()
            .filter(
                e ->
                    com.res.hms.clinical.domain.ExceptionType.MODIFIED.equals(e.getType())
                        || com.res.hms.clinical.domain.ExceptionType.ADDED.equals(e.getType()))
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
              .filter(s -> s.getDayOfWeek().equals(dbDay))
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

  @Transactional(readOnly = true)
  public boolean isFullDayOff(UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return availabilityExceptionRepository
        .findByDoctorIdAndHospitalIdAndDate(doctorId, hospitalId, date)
        .stream()
        .anyMatch(e -> com.res.hms.clinical.domain.ExceptionType.FULL_DAY_OFF.equals(e.getType()));
  }

  private void generateSlots(
      LocalTime start,
      LocalTime end,
      int duration,
      java.util.Set<LocalTime> bookedTimes,
      java.time.LocalDate date,
      java.time.LocalDate today,
      LocalTime now,
      List<com.res.hms.iam.application.dto.DoctorSlotDTO> allSlots) {
    LocalTime current = start;
    if (end.equals(LocalTime.MIDNIGHT) && !start.equals(LocalTime.MIDNIGHT)) {
        // Handle slots ending at midnight correctly
    }
    
    while (true) {
      LocalTime slotEnd = current.plusMinutes(duration);
      
      // Stop if we reached or exceeded the end time
      // Special case: if end is 00:00, it means end of day
      if (!end.equals(LocalTime.MIDNIGHT)) {
          if (slotEnd.isAfter(end)) break;
      } else {
          // If end is midnight, and we just wrapped to midnight, we are done
          if (slotEnd.equals(LocalTime.MIDNIGHT)) {
              // Add this last slot and break
              addSlot(current, slotEnd, bookedTimes, date, today, now, allSlots);
              break;
          }
          // If we wrapped past midnight without hitting it exactly (e.g. 23:50 + 15 = 00:05)
          if (slotEnd.isBefore(current)) break;
      }

      addSlot(current, slotEnd, bookedTimes, date, today, now, allSlots);
      
      if (slotEnd.equals(end)) break;
      
      current = slotEnd;
    }
  }

  private void addSlot(
      LocalTime current,
      LocalTime slotEnd,
      java.util.Set<LocalTime> bookedTimes,
      java.time.LocalDate date,
      java.time.LocalDate today,
      LocalTime now,
      List<com.res.hms.iam.application.dto.DoctorSlotDTO> allSlots) {
    boolean isPast = date.isBefore(today) || (date.equals(today) && current.isBefore(now));
    boolean isBooked = bookedTimes.contains(current);

    String status = "AVAILABLE";
    if (isPast) {
      status = "SESSION_EXPIRED";
    } else if (isBooked) {
      status = "BOOKED";
    }

    allSlots.add(
        com.res.hms.iam.application.dto.DoctorSlotDTO.builder()
            .startTime(current)
            .endTime(slotEnd)
            .available(!isPast && !isBooked)
            .status(status)
            .build());
  }

  private int countBookingsInRange(
      java.util.List<com.res.hms.clinical.domain.Appointment> appointments,
      LocalTime start,
      LocalTime end,
      java.time.ZoneId zoneId) {
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

  private final com.res.hms.iam.infrastructure.repository.SpecializationRepository specializationRepository;

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
    if (info.getSpecializations() != null) {
      doctorProfile.setSpecializations(info.getSpecializations().stream()
          .map(name -> specializationRepository.findByName(name)
              .orElseThrow(() -> new com.res.hms.core.exception.ApiException(
                  "NOT_FOUND", "Specialization " + name + " not found in catalog", org.springframework.http.HttpStatus.BAD_REQUEST)))
          .collect(java.util.stream.Collectors.toSet()));
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

    doctorProfileRepository.save(doctorProfile);
    user.setDoctorProfile(doctorProfile);
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
            s.setDayOfWeek(dto.getDayOfWeek());
            s.setStartTime(dto.getStartTime());
            s.setEndTime(dto.getEndTime());
            s.setSlotDurationMinutes(dto.getSlotDurationMinutes());
            s.setMaxPatients(dto.getMaxPatients());
            return s;
          },
          (existing, dto) -> {
            existing.setDayOfWeek(dto.getDayOfWeek());
            existing.setStartTime(dto.getStartTime());
            existing.setEndTime(dto.getEndTime());
            existing.setSlotDurationMinutes(dto.getSlotDurationMinutes());
            existing.setMaxPatients(dto.getMaxPatients());
          },
          s -> {
            scheduleRepository.delete(s);
            existingSchedules.remove(s);
          });
      // Filter out any remaining nulls or deleted items before saveAll to be safe
      List<DoctorSchedule> toSave = existingSchedules.stream()
          .filter(Objects::nonNull)
          .toList();
      scheduleRepository.saveAll(toSave);
    }

    if (exceptions != null && assignment.getUser().getDoctorProfile() != null) {
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
            ex.setDate(dto.getDate());
            ex.setType(dto.getType());
            ex.setStartTime(dto.getStartTime());
            ex.setEndTime(dto.getEndTime());
            ex.setMode(dto.getMode());
            ex.setSlotDurationMinutes(dto.getSlotDurationMinutes());
            ex.setNote(dto.getNote());
            return ex;
          },
          (existing, dto) -> {
            existing.setDate(dto.getDate());
            existing.setType(dto.getType());
            existing.setStartTime(dto.getStartTime());
            existing.setEndTime(dto.getEndTime());
            existing.setMode(dto.getMode());
            existing.setSlotDurationMinutes(dto.getSlotDurationMinutes());
            existing.setNote(dto.getNote());
          },
          ex -> {
            availabilityExceptionRepository.delete(ex);
            existingExceptions.remove(ex);
          });
      // Filter out any remaining nulls or deleted items before saveAll to be safe
      List<DoctorAvailabilityException> toSave = existingExceptions.stream()
          .filter(Objects::nonNull)
          .toList();
      availabilityExceptionRepository.saveAll(toSave);
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
      return java.util.Collections.emptyList();
    }
    return availabilityExceptionRepository.findByDoctorIdAndHospitalId(profile.getId(), hospitalId)
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
