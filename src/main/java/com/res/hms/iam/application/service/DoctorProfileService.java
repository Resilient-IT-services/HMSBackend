package com.res.hms.iam.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.finance.infrastructure.repository.InvoiceItemRepository;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.AvailabilityExceptionDTO;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.dto.DoctorHospitalDetailsDTO;
import com.res.hms.iam.application.dto.DoctorRevenueSummaryDTO;
import com.res.hms.iam.application.dto.DoctorSlotDTO;
import com.res.hms.iam.application.dto.ScheduleDTO;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.SpecializationRepository;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
  private final InvoiceItemRepository invoiceItemRepository;
  private final SpecializationRepository specializationRepository;
  private final DoctorAvailabilityService doctorAvailabilityService;

  @Transactional(readOnly = true)
  public DoctorRevenueSummaryDTO getRevenueSummary(UUID doctorId, Instant from, Instant to) {
    List<Object[]> results = invoiceItemRepository.getDoctorRevenueSummary(doctorId, from, to);
    Object[] result = results != null && !results.isEmpty() ? results.get(0) : null;
    long count = 0;
    BigDecimal total = BigDecimal.ZERO;
    if (result != null) {
      if (result[0] instanceof Number n) {
        count = n.longValue();
      }
      if (result[1] instanceof BigDecimal b) {
        total = b;
      }
    }
    BigDecimal average =
        count > 0
            ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    return DoctorRevenueSummaryDTO.builder()
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
    return doctorAvailabilityService.getAvailableSlots(
        doctorProfile, assignment, date, getHospitalZoneId(hospitalId));
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
    return doctorAvailabilityService.getSlots(
        doctorProfile, assignment, date, getHospitalZoneId(hospitalId));
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
    return doctorAvailabilityService.isFullDayOff(doctorId, hospitalId, date);
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
