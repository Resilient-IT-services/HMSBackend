package com.res.hms.iam.application.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.iam.application.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.domain.ReceptionistHospitalDetails;
import com.res.hms.iam.domain.ReceptionistProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.ReceptionistHospitalDetailsRepository;
import com.res.hms.iam.infrastructure.repository.ReceptionistProfileRepository;

import lombok.RequiredArgsConstructor;

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
    receptionistHospitalDetailsRepository.save(det);
    a.setReceptionistDetails(det);
  }
}
