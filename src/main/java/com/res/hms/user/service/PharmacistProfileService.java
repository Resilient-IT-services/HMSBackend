package com.res.hms.user.service;

import com.res.hms.domain.PharmacistProfile;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.domain.PharmacistHospitalDetails;
import com.res.hms.repository.PharmacistHospitalDetailsRepository;
import com.res.hms.repository.PharmacistProfileRepository;
import com.res.hms.user.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.time.LocalDateTime;
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
  public void updateProfile(UserAccount user, UserDetailsDTO.PharmacistInfo info, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.PHARMACIST) && info != null) {
      PharmacistProfile profile = pharmacistProfileRepository.findById(user.getId())
          .orElseGet(() -> {
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
  public void updateHospitalDetails(UserRoleAssignment a, PharmacistHospitalDetailsDTO p) {
    if (p == null) {
      return;
    }
    PharmacistHospitalDetails det = pharmacistHospitalDetailsRepository.findById(a.getId())
        .orElseGet(() -> {
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
    det.setUpdatedAt(LocalDateTime.now());
    pharmacistHospitalDetailsRepository.save(det);
    a.setPharmacistDetails(det);
  }
}
