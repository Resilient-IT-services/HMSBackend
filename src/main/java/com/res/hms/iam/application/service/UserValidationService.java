package com.res.hms.iam.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.infrastructure.repository.LabAdminProfileRepository;
import com.res.hms.iam.infrastructure.repository.NurseProfileRepository;
import com.res.hms.iam.infrastructure.repository.PharmacistProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserValidationService {

  private final DoctorProfileRepository doctorProfileRepository;
  private final NurseProfileRepository nurseProfileRepository;
  private final PharmacistProfileRepository pharmacistProfileRepository;
  private final LabAdminProfileRepository labAdminProfileRepository;

  public void validateProfileUniqueness(UserDetailsDTO request, UUID userId) {
    validateDoctorLicenseRequired(request, userId);
    String doctorLicense =
        normalizeProfileIdentifier(
            request.getDoctorInfo() != null ? request.getDoctorInfo().getLicenseNumber() : null);
    if (doctorLicense != null) {
      boolean exists =
          (userId == null)
              ? doctorProfileRepository.existsByLicenseNumber(doctorLicense)
              : doctorProfileRepository.existsByLicenseNumberAndUserIdNot(doctorLicense, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION", "Doctor license number already exists", HttpStatus.CONFLICT);
      }
    }
    String nurseLicense =
        normalizeProfileIdentifier(
            request.getNurseInfo() != null ? request.getNurseInfo().getNursingLicenseNo() : null);
    if (nurseLicense != null) {
      boolean exists =
          (userId == null)
              ? nurseProfileRepository.existsByNursingLicenseNo(nurseLicense)
              : nurseProfileRepository.existsByNursingLicenseNoAndUserIdNot(nurseLicense, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "NurseProfile license number already exists",
            HttpStatus.CONFLICT);
      }
    }
    String pharmacistRegistration =
        normalizeProfileIdentifier(
            request.getPharmacistInfo() != null ? request.getPharmacistInfo().getPciRegNo() : null);
    if (pharmacistRegistration != null) {
      boolean exists =
          (userId == null)
              ? pharmacistProfileRepository.existsByPciRegNo(pharmacistRegistration)
              : pharmacistProfileRepository.existsByPciRegNoAndUserIdNot(
                  pharmacistRegistration, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Pharmacist PCI registration number already exists",
            HttpStatus.CONFLICT);
      }
    }
    String labAdminLicense =
        normalizeProfileIdentifier(
            request.getLabAdminInfo() != null ? request.getLabAdminInfo().getLicenseNo() : null);
    if (labAdminLicense != null) {
      boolean exists =
          (userId == null)
              ? labAdminProfileRepository.existsByLicenseNo(labAdminLicense)
              : labAdminProfileRepository.existsByLicenseNoAndUserIdNot(labAdminLicense, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Lab Admin license number already exists",
            HttpStatus.CONFLICT);
      }
    }
  }

  private String normalizeProfileIdentifier(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void validateDoctorLicenseRequired(UserDetailsDTO request, UUID userId) {
    if (!hasDoctorRole(request)) {
      return;
    }

    boolean createRequest = userId == null;
    boolean licenseSubmitted =
        request.getDoctorInfo() != null && request.getDoctorInfo().getLicenseNumber() != null;
    String doctorLicense =
        normalizeProfileIdentifier(
            request.getDoctorInfo() != null ? request.getDoctorInfo().getLicenseNumber() : null);

    if ((createRequest || licenseSubmitted) && doctorLicense == null) {
      throw new ApiException(
          "VALIDATION_ERROR", "Doctor license number is required", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean hasDoctorRole(UserDetailsDTO request) {
    if (request.getMemberships() == null) {
      return false;
    }

    return request.getMemberships().stream()
        .filter(membership -> membership.getRoleAssignments() != null)
        .flatMap(membership -> membership.getRoleAssignments().stream())
        .anyMatch(roleAssignment -> "DOCTOR".equals(roleAssignment.getRoleCode()));
  }
}
