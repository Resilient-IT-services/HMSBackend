package com.res.hms.iam.application.service;

import com.res.hms.clinical.domain.DoctorAvailabilityOverride;
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
  private final com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityOverrideRepository
      availabilityOverrideRepository;

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
      updateOverrides(assignment, exceptions);
    }
    if (details != null) {
      updateDoctorHospitalDetails(assignment, details);
    }
  }

  @Transactional
  public void deleteHospitalSpecificData(UserContract assignment) {
    scheduleRepository.deleteByAssignmentId(assignment.getId());
    if (assignment.getUser().getDoctorProfile() != null) {
      availabilityOverrideRepository.deleteByDoctorIdAndHospitalId(
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
    return availabilityOverrideRepository
        .findByDoctorIdAndHospitalId(profile.getId(), hospitalId)
        .stream()
        .map(this::toOverrideDTO)
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

  private void updateOverrides(UserContract assignment, List<AvailabilityExceptionDTO> exceptions) {
    DoctorProfile doctorProfile = assignment.getUser().getDoctorProfile();
    List<DoctorAvailabilityOverride> existingOverrides =
        availabilityOverrideRepository.findByDoctorIdAndHospitalId(
            doctorProfile.getId(), assignment.getHospitalId());

    com.res.hms.core.util.CollectionMergeUtil.merge(
        existingOverrides,
        exceptions,
        DoctorAvailabilityOverride::getId,
        AvailabilityExceptionDTO::getId,
        dto -> newOverride(assignment, doctorProfile, dto),
        this::applyOverrideDto,
        ex -> {
          availabilityOverrideRepository.delete(ex);
          existingOverrides.remove(ex);
        });
    availabilityOverrideRepository.saveAll(
        existingOverrides.stream().filter(Objects::nonNull).toList());
  }

  private DoctorAvailabilityOverride newOverride(
      UserContract assignment, DoctorProfile doctorProfile, AvailabilityExceptionDTO dto) {
    DoctorAvailabilityOverride override = new DoctorAvailabilityOverride();
    override.setDoctorId(doctorProfile.getId());
    override.setHospitalId(assignment.getHospitalId());
    applyOverrideDto(override, dto);
    return override;
  }

  private void applyOverrideDto(DoctorAvailabilityOverride override, AvailabilityExceptionDTO dto) {
    override.setDate(dto.getDate());
    override.setType(dto.getType());
    override.setStartTime(dto.getStartTime());
    override.setEndTime(dto.getEndTime());
    override.setMode(dto.getMode());
    override.setSlotDurationMinutes(dto.getSlotDurationMinutes());
    override.setNote(dto.getNote());
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

  private AvailabilityExceptionDTO toOverrideDTO(DoctorAvailabilityOverride override) {
    return AvailabilityExceptionDTO.builder()
        .id(override.getId())
        .date(override.getDate())
        .type(override.getType())
        .startTime(override.getStartTime())
        .endTime(override.getEndTime())
        .mode(override.getMode())
        .slotDurationMinutes(override.getSlotDurationMinutes())
        .note(override.getNote())
        .build();
  }
}
