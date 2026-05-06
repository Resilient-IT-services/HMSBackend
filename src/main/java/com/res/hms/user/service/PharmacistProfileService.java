package com.res.hms.user.service;

import com.res.hms.domain.PharmacistHospitalDetails;
import com.res.hms.domain.PharmacistProfile;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.pharmacy.repository.PharmacistHospitalDetailsRepository;
import com.res.hms.pharmacy.repository.PharmacistProfileRepository;
import com.res.hms.user.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.pharmacy.mapper.PharmacistMapper;

@Service
@RequiredArgsConstructor
public class PharmacistProfileService {

  private final PharmacistProfileRepository pharmacistProfileRepository;
  private final PharmacistHospitalDetailsRepository pharmacistHospitalDetailsRepository;
  private final PharmacistMapper pharmacistMapper;

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

      pharmacistMapper.updateEntity(info, profile);
      pharmacistProfileRepository.save(profile);
      user.setPharmacistProfile(profile);
    }
  }

  @Transactional
  public void updateHospitalDetails(UserRoleAssignment a, PharmacistHospitalDetailsDTO p) {
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

    pharmacistMapper.updateEntity(p, det);
    pharmacistHospitalDetailsRepository.save(det);
    a.setPharmacistDetails(det);
  }
}
