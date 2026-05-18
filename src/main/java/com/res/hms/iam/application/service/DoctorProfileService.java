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
                    new com.res.hms.core.exception.ApiException(
                        "NOT_FOUND",
                        "DoctorProfile not found",
                        org.springframework.http.HttpStatus.NOT_FOUND));

    if (!doctorProfile.getUser().isActive()) {
      throw new com.res.hms.core.exception.ApiException(
          "BUSINESS_RULE_VIOLATION", "Doctor is inactive", org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    UserAccount user = doctorProfile.getUser();
    UUID actualDoctorId = doctorProfile.getId();
    // 2. Find the relevant assignment for this hospital
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

    // 3. Check for exceptions
    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            actualDoctorId, hospitalId, date);

    if (exceptions.stream().anyMatch(e -> "FULL_DAY_OFF".equals(e.getType()))) {
      return new DoctorAvailabilityResponse(java.util.Collections.emptyList());
    }

    // 4. Fetch existing appointments
    java.util.List<com.res.hms.clinical.domain.Appointment> appointments =
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
          scheduleRepository.findByAssignmentId(assignment.getId()).stream()
              .filter(s -> s.getDayOfWeek().equals(dbDay))
              .toList();

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
  public List<com.res.hms.iam.application.dto.DoctorSlotDTO> getSlots(
      UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    short dbDay = (short) date.getDayOfWeek().getValue();

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

    if (exceptions.stream().anyMatch(e -> "FULL_DAY_OFF".equals(e.getType()))) {
      return java.util.Collections.emptyList();
    }

    java.util.List<com.res.hms.clinical.domain.Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(actualDoctorId, date);
    java.util.Set<LocalTime> bookedTimes =
        appointments.stream()
            .map(a -> a.getAppointmentTime().toLocalTime())
            .collect(java.util.stream.Collectors.toSet());

    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalTime now = java.time.LocalTime.now();

    List<com.res.hms.iam.application.dto.DoctorSlotDTO> allSlots = new java.util.ArrayList<>();
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
          scheduleRepository.findByAssignmentId(assignment.getId()).stream()
              .filter(s -> s.getDayOfWeek().equals(dbDay))
              .toList();
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
      List<com.res.hms.iam.application.dto.DoctorSlotDTO> allSlots) {
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
          com.res.hms.iam.application.dto.DoctorSlotDTO.builder()
              .startTime(current)
              .endTime(slotEnd)
              .available(!isPast && !isBooked)
              .status(status)
              .build());
      current = slotEnd;
    }
  }

  private int countBookingsInRange(
      java.util.List<com.res.hms.clinical.domain.Appointment> appointments,
      LocalTime start,
      LocalTime end) {
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
                    .dayOfWeek(s.getDayOfWeek().shortValue())
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
