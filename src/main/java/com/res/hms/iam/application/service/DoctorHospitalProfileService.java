package com.res.hms.iam.application.service;

import com.res.hms.clinical.domain.DoctorAvailabilityException;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.iam.application.dto.AvailabilityExceptionDTO;
import com.res.hms.iam.application.dto.DoctorHospitalDetailsDTO;
import com.res.hms.iam.application.dto.ScheduleDTO;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorHospitalProfileService {

  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final DoctorProfileRepository doctorProfileRepository;
  private final com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository
      scheduleRepository;
  private final com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityExceptionRepository
      availabilityExceptionRepository;

  @Transactional
  public void updateHospitalDetails(
      UserContract assignment,
      DoctorHospitalDetailsDTO details,
      List<ScheduleDTO> schedules,
      List<AvailabilityExceptionDTO> exceptions) {
    if (schedules != null) {
      updateSchedules(assignment, schedules);
    }
    if (exceptions != null && assignment.getUser().getDoctorProfile() != null) {
      updateExceptions(assignment, exceptions);
    }
    if (details != null) {
      updateDoctorHospitalDetails(assignment, details);
    }
  }

  @Transactional
  public void deleteHospitalSpecificData(UserContract assignment) {
    scheduleRepository.deleteByAssignmentId(assignment.getId());
    if (assignment.getUser().getDoctorProfile() != null) {
      availabilityExceptionRepository.deleteByDoctorIdAndHospitalId(
          assignment.getUser().getDoctorProfile().getId(), assignment.getHospitalId());
    }
  }

  @Transactional(readOnly = true)
  public List<ScheduleDTO> getScheduleDTOs(UUID assignmentId) {
    return scheduleRepository.findByAssignmentId(assignmentId).stream()
        .map(this::toScheduleDTO)
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
        .map(this::toExceptionDTO)
        .toList();
  }

  private void updateSchedules(UserContract assignment, List<ScheduleDTO> schedules) {
    List<DoctorSchedule> existingSchedules =
        scheduleRepository.findByAssignmentId(assignment.getId());
    com.res.hms.core.util.CollectionMergeUtil.merge(
        existingSchedules,
        schedules,
        DoctorSchedule::getId,
        ScheduleDTO::getId,
        dto -> newSchedule(assignment, dto),
        this::applyScheduleDto,
        s -> {
          scheduleRepository.delete(s);
          existingSchedules.remove(s);
        });
    scheduleRepository.saveAll(existingSchedules.stream().filter(Objects::nonNull).toList());
  }

  private DoctorSchedule newSchedule(UserContract assignment, ScheduleDTO dto) {
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setAssignmentId(assignment.getId());
    applyScheduleDto(schedule, dto);
    return schedule;
  }

  private void applyScheduleDto(DoctorSchedule schedule, ScheduleDTO dto) {
    schedule.setDayOfWeek(dto.getDayOfWeek());
    schedule.setStartTime(dto.getStartTime());
    schedule.setEndTime(dto.getEndTime());
    schedule.setSlotDurationMinutes(dto.getSlotDurationMinutes());
    schedule.setMaxPatients(dto.getMaxPatients());
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
        dto -> newException(assignment, doctorProfile, dto),
        this::applyExceptionDto,
        ex -> {
          availabilityExceptionRepository.delete(ex);
          existingExceptions.remove(ex);
        });
    availabilityExceptionRepository.saveAll(
        existingExceptions.stream().filter(Objects::nonNull).toList());
  }

  private DoctorAvailabilityException newException(
      UserContract assignment, DoctorProfile doctorProfile, AvailabilityExceptionDTO dto) {
    DoctorAvailabilityException exception = new DoctorAvailabilityException();
    exception.setDoctorId(doctorProfile.getId());
    exception.setHospitalId(assignment.getHospitalId());
    applyExceptionDto(exception, dto);
    return exception;
  }

  private void applyExceptionDto(
      DoctorAvailabilityException exception, AvailabilityExceptionDTO dto) {
    exception.setDate(dto.getDate());
    exception.setType(dto.getType());
    exception.setStartTime(dto.getStartTime());
    exception.setEndTime(dto.getEndTime());
    exception.setMode(dto.getMode());
    exception.setSlotDurationMinutes(dto.getSlotDurationMinutes());
    exception.setNote(dto.getNote());
  }

  private void updateDoctorHospitalDetails(UserContract assignment, DoctorHospitalDetailsDTO dto) {
    DoctorHospitalDetails details =
        doctorHospitalDetailsRepository
            .findById(assignment.getId())
            .orElseGet(() -> newHospitalDetails(assignment));
    applyHospitalDetails(details, dto);
    doctorHospitalDetailsRepository.save(details);
    assignment.setDoctorDetails(details);
  }

  private DoctorHospitalDetails newHospitalDetails(UserContract assignment) {
    DoctorHospitalDetails details = new DoctorHospitalDetails();
    details.setAssignment(assignment);
    return details;
  }

  private void applyHospitalDetails(DoctorHospitalDetails details, DoctorHospitalDetailsDTO dto) {
    if (dto.getConsultationFee() != null) {
      details.setConsultationFee(dto.getConsultationFee());
    }
    if (dto.getFollowUpDays() != null) {
      details.setFollowUpDays(dto.getFollowUpDays());
    }
    if (dto.getCommissionType() != null) {
      details.setCommissionType(dto.getCommissionType());
    }
    if (dto.getCommissionValue() != null) {
      details.setCommissionValue(dto.getCommissionValue());
    }
    details.setVisitingConsultant(dto.isVisitingConsultant());
  }

  private ScheduleDTO toScheduleDTO(DoctorSchedule schedule) {
    return ScheduleDTO.builder()
        .id(schedule.getId())
        .dayOfWeek(schedule.getDayOfWeek())
        .startTime(schedule.getStartTime())
        .endTime(schedule.getEndTime())
        .slotDurationMinutes(schedule.getSlotDurationMinutes())
        .maxPatients(schedule.getMaxPatients())
        .build();
  }

  private AvailabilityExceptionDTO toExceptionDTO(DoctorAvailabilityException exception) {
    return AvailabilityExceptionDTO.builder()
        .id(exception.getId())
        .date(exception.getDate())
        .type(exception.getType())
        .startTime(exception.getStartTime())
        .endTime(exception.getEndTime())
        .mode(exception.getMode())
        .slotDurationMinutes(exception.getSlotDurationMinutes())
        .note(exception.getNote())
        .build();
  }
}
