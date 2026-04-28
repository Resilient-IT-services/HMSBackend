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
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorProfileService {

  private final DoctorProfileRepository DoctorProfileRepository;
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final HospitalRepository hospitalRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  private final com.res.hms.repository.UserRoleAssignmentRepository assignmentRepository;
  private final com.res.hms.repository.DoctorScheduleRepository scheduleRepository;
  private final com.res.hms.repository.DoctorAvailabilityExceptionRepository
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
        DoctorProfileRepository.findById(doctorId)
            .or(() -> DoctorProfileRepository.findByUserId(doctorId))
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
        DoctorProfileRepository.findByUserId(user.getId())
            .orElseGet(
                () -> {
                  DoctorProfile d = new DoctorProfile();
                  d.setUser(user);
                  return d;
                });
    if (info.getSpecialization() != null) {
      doctorProfile.setSpecialization(info.getSpecialization());
    }
    if (info.getBio() != null) {
      doctorProfile.setBio(info.getBio());
    }
    if (info.getLicenseNumber() != null) {
      doctorProfile.setLicenseNumber(info.getLicenseNumber());
    }

    DoctorProfileRepository.save(doctorProfile);
    user.setDoctorProfile(doctorProfile);
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
            DoctorSchedule s = new DoctorSchedule();
            s.setAssignment(assignment);
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
            DoctorAvailabilityException ex = new DoctorAvailabilityException();
            ex.setDoctorProfile(doctorProfile);
            ex.setHospital(hospital);
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
        id, hospitalId, DoctorProfileRepository::findByIdAndHospitalId, "DoctorProfile");
  }
}
