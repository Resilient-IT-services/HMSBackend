package com.res.hms.user.service;

import com.res.hms.domain.ReceptionistHospitalDetails;
import com.res.hms.domain.ReceptionistProfile;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.ReceptionistHospitalDetailsRepository;
import com.res.hms.repository.ReceptionistProfileRepository;
import com.res.hms.user.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceptionistProfileService {

  private final ReceptionistProfileRepository receptionistProfileRepository;
  private final ReceptionistHospitalDetailsRepository receptionistHospitalDetailsRepository;

  @Transactional
  public void updateProfile(
      UserAccount user, UserDetailsDTO.ReceptionistInfo info, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.RECEPTIONIST) && info != null) {
      ReceptionistProfile profile =
          receptionistProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    ReceptionistProfile p = new ReceptionistProfile();
                    p.setUser(user);
                    return p;
                  });
      if (info.getLanguagesSpoken() != null) {
        profile.setLanguagesSpoken(info.getLanguagesSpoken());
      }
      profile.setInsuranceSpecialist(info.isInsuranceSpecialist());
      receptionistProfileRepository.save(profile);
      user.setReceptionistProfile(profile);
    }
  }

  @Transactional
  public void updateHospitalDetails(UserRoleAssignment a, ReceptionistHospitalDetailsDTO r) {
    if (r == null) {
      return;
    }
    ReceptionistHospitalDetails det =
        receptionistHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  ReceptionistHospitalDetails newDet = new ReceptionistHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });
    if (r.getDeskLocation() != null) {
      det.setDeskLocation(r.getDeskLocation());
    }
    if (r.getEmergencyContactNumber() != null) {
      det.setEmergencyContactNumber(r.getEmergencyContactNumber());
    }
    det.setUpdatedAt(LocalDateTime.now());
    receptionistHospitalDetailsRepository.save(det);
    a.setReceptionistDetails(det);
  }
}
