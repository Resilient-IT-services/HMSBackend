package com.res.hms.user.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.doctor.repository.DoctorProfileRepository;
import com.res.hms.nurse.repository.NurseProfileRepository;
import com.res.hms.repository.LabTechProfileRepository;
import com.res.hms.repository.PharmacistProfileRepository;
import com.res.hms.user.dto.UserDetailsDTO;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserValidationService {

  private final DoctorProfileRepository DoctorProfileRepository;
  private final NurseProfileRepository NurseProfileRepository;
  private final PharmacistProfileRepository pharmacistProfileRepository;
  private final LabTechProfileRepository labTechProfileRepository;

  public void validateProfileUniqueness(UserDetailsDTO request, UUID userId) {
    if (request.getDoctorInfo() != null && request.getDoctorInfo().getLicenseNumber() != null) {
      String ln = request.getDoctorInfo().getLicenseNumber();
      boolean exists =
          (userId == null)
              ? DoctorProfileRepository.existsByLicenseNumber(ln)
              : DoctorProfileRepository.existsByLicenseNumberAndUserIdNot(ln, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "DoctorProfile license number already exists",
            HttpStatus.CONFLICT);
      }
    }
    if (request.getNurseInfo() != null && request.getNurseInfo().getNursingLicenseNo() != null) {
      String ln = request.getNurseInfo().getNursingLicenseNo();
      boolean exists =
          (userId == null)
              ? NurseProfileRepository.existsByNursingLicenseNo(ln)
              : NurseProfileRepository.existsByNursingLicenseNoAndUserIdNot(ln, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "NurseProfile license number already exists",
            HttpStatus.CONFLICT);
      }
    }
    if (request.getPharmacistInfo() != null && request.getPharmacistInfo().getPciRegNo() != null) {
      String reg = request.getPharmacistInfo().getPciRegNo();
      boolean exists =
          (userId == null)
              ? pharmacistProfileRepository.existsByPciRegNo(reg)
              : pharmacistProfileRepository.existsByPciRegNoAndUserIdNot(reg, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Pharmacist PCI registration number already exists",
            HttpStatus.CONFLICT);
      }
    }
    if (request.getLabTechInfo() != null && request.getLabTechInfo().getLicenseNo() != null) {
      String ln = request.getLabTechInfo().getLicenseNo();
      boolean exists =
          (userId == null)
              ? labTechProfileRepository.existsByLicenseNo(ln)
              : labTechProfileRepository.existsByLicenseNoAndUserIdNot(ln, userId);
      if (exists) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Lab Tech license number already exists",
            HttpStatus.CONFLICT);
      }
    }
  }
}
