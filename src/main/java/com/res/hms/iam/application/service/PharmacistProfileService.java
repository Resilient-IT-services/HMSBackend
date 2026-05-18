package com.res.hms.iam.application.service;

import com.res.hms.iam.application.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.domain.PharmacistHospitalDetails;
import com.res.hms.iam.domain.PharmacistProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.PharmacistHospitalDetailsRepository;
import com.res.hms.iam.infrastructure.repository.PharmacistProfileRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PharmacistProfileService {

  private final PharmacistProfileRepository pharmacistProfileRepository;
  private final PharmacistHospitalDetailsRepository pharmacistHospitalDetailsRepository;

  @Transactional
  public void updateProfile(
      UserAccount user, UserDetailsDTO.PharmacistInfo info, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.PHARMACIST) && info != null) {
      PharmacistProfile profile =
          pharmacistProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    PharmacistProfile p = new PharmacistProfile();
                    p.setUser(user);
                    return p;
                  });
      if (info.getPciRegNo() != null) {
        profile.setPciRegNo(info.getPciRegNo());
      }
      if (info.getQualification() != null) {
        profile.setQualification(info.getQualification());
      }
      pharmacistProfileRepository.save(profile);
      user.setPharmacistProfile(profile);
    }
  }

  @Transactional
  public void updateHospitalDetails(UserContract a, PharmacistHospitalDetailsDTO p) {
    if (p == null) {
      return;
    }
    PharmacistHospitalDetails det =
        pharmacistHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  PharmacistHospitalDetails newDet = new PharmacistHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });
    if (p.getCounterLocation() != null) {
      det.setCounterLocation(p.getCounterLocation());
    }
    if (p.getPharmacyWing() != null) {
      det.setPharmacyWing(p.getPharmacyWing());
    }
    if (p.getPharmacyLicenseNo() != null) {
      det.setPharmacyLicenseNo(p.getPharmacyLicenseNo());
    }
    pharmacistHospitalDetailsRepository.save(det);
    a.setPharmacistDetails(det);
  }
}
