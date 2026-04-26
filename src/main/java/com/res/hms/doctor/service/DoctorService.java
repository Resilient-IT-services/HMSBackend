package com.res.hms.doctor.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.dto.AvailabilityExceptionDTO;
import com.res.hms.doctor.dto.DoctorAvailabilityResponse;
import com.res.hms.doctor.dto.DoctorResponse;
import com.res.hms.doctor.dto.ScheduleDTO;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.DoctorAvailabilityException;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.DoctorSchedule;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

  private final DoctorRepository doctorRepository;
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final HospitalRepository hospitalRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  private final com.res.hms.repository.DoctorScheduleRepository scheduleRepository;
  private final com.res.hms.repository.DoctorAvailabilityExceptionRepository
      availabilityExceptionRepository;
  private final com.res.hms.appointment.repository.AppointmentRepository appointmentRepository;

  @Transactional(readOnly = true)
  public DoctorAvailabilityResponse getAvailableSlots(UUID doctorId, java.time.LocalDate date) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    // 1. Check for exceptions
    List<DoctorAvailabilityException> exceptions =
        availabilityExceptionRepository.findByDoctorIdAndHospitalIdAndDate(
            doctorId, hospitalId, date);

    if (exceptions.stream().anyMatch(e -> "FULL_DAY_OFF".equals(e.getType()))) {
      return new DoctorAvailabilityResponse(java.util.Collections.emptyList());
    }

    // 2. Fetch existing appointments to count per session
    java.util.List<com.res.hms.domain.Appointment> appointments =
        appointmentRepository.findByDoctorIdAndDate(doctorId, date);

    // 3. Determine sessions
    List<DoctorAvailabilityResponse.SessionInfo> sessions = new java.util.ArrayList<>();
    Optional<DoctorAvailabilityException> override =
        exceptions.stream()
            .filter(e -> "MODIFIED".equals(e.getType()) || "ADDED".equals(e.getType()))
            .findFirst();

    if (override.isPresent()) {
      var o = override.get();
      int count = countBookingsInRange(appointments, o.getStartTime(), o.getEndTime());
      int duration = o.getSlotDurationMinutes() != null ? o.getSlotDurationMinutes() : 15;

      sessions.add(
          new DoctorAvailabilityResponse.SessionInfo(
              null,
              o.getStartTime(),
              o.getEndTime(),
              null, // No max limit on exceptions unless added to table
              count,
              true, // Exceptions usually mean available
              calculateSuggestedTime(o.getStartTime(), count, duration)));
    } else {
      int dbDay = date.getDayOfWeek().getValue() == 7 ? 0 : date.getDayOfWeek().getValue();
      List<DoctorSchedule> baseSchedules =
          scheduleRepository.findByDoctorAndHospitalAndDay(doctorId, hospitalId, dbDay);

      for (DoctorSchedule s : baseSchedules) {
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
    Doctor doctor =
        doctorRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  Doctor d = new Doctor();
                  d.setUser(user);
                  return d;
                });
    if (info.getSpecialization() != null) {
      doctor.setSpecialization(info.getSpecialization());
    }
    if (info.getBio() != null) {
      doctor.setBio(info.getBio());
    }
    if (info.getLicenseNumber() != null) {
      doctor.setLicenseNumber(info.getLicenseNumber());
    }

    doctorRepository.save(doctor);
    user.setDoctor(doctor);
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

    if (exceptions != null && assignment.getUser().getDoctor() != null) {
      Doctor doctor = assignment.getUser().getDoctor();
      Hospital hospital =
          resourceLookup.findRequired(
              assignment.getHospitalId(), hospitalRepository::findById, "Hospital");

      com.res.hms.common.util.CollectionMergeUtil.merge(
          doctor.getAvailabilityExceptions(),
          exceptions,
          DoctorAvailabilityException::getId,
          AvailabilityExceptionDTO::getId,
          dto -> {
            DoctorAvailabilityException ex = new DoctorAvailabilityException();
            ex.setDoctor(doctor);
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
          doctor.getAvailabilityExceptions()::remove);
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
    det.setUpdatedAt(LocalDateTime.now());
    doctorHospitalDetailsRepository.save(det);
    assignment.setDoctorDetails(det);
  }

  public PageResponse<DoctorResponse> list(int page, int size) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        doctorRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
            .map(this::toResponse));
  }

  public Doctor requireDoctor(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, doctorRepository::findByIdAndHospitalId, "Doctor");
  }

  private DoctorResponse toResponse(Doctor doctor) {
    UserAccount user = doctor.getUser();
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();

    BigDecimal consultationFee = BigDecimal.ZERO;
    Integer followUpDays = 0;

    if (activeHospitalId != null && user != null) {
      Optional<DoctorHospitalDetails> details =
          doctorHospitalDetailsRepository.findByUserIdAndHospitalId(user.getId(), activeHospitalId);
      if (details.isPresent()) {
        consultationFee = details.get().getConsultationFee();
        followUpDays = details.get().getFollowUpDays();
      }
    }

    return new DoctorResponse(
        doctor.getId(),
        user != null ? user.getFullName() : "N/A",
        user != null ? user.getGender() : null,
        user != null ? user.getDob() : null,
        user != null ? user.getPhone() : null,
        doctor.getSpecialization(),
        consultationFee,
        followUpDays,
        doctor.getEducation(),
        doctor.getCreatedAt());
  }
}
