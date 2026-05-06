package com.res.hms.nurse.service;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.NurseHospitalDetails;
import com.res.hms.domain.NurseProfile;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseProfileRepository;
import com.res.hms.nurse.repository.NurseHospitalDetailsRepository;
import com.res.hms.user.repository.UserAccountRepository;
import com.res.hms.user.dto.NurseHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.nurse.mapper.NurseMapper;

@Service
@RequiredArgsConstructor
public class NurseProfileService {

  private final NurseProfileRepository nurseProfileRepository;
  private final NurseHospitalDetailsRepository nurseHospitalDetailsRepository;
  private final UserAccountRepository userAccountRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;
  private final NurseMapper nurseMapper;

  @Transactional
  public void updateProfile(UserAccount user, UserDetailsDTO.NurseInfo info) {
    if (info == null) {
      return;
    }
    NurseProfile profile =
        nurseProfileRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  NurseProfile n = new NurseProfile();
                  n.setUser(user);
                  return n;
                });

    nurseMapper.updateEntity(info, profile);
    nurseProfileRepository.save(profile);
    user.setNurseProfile(profile);
  }

  @Transactional
  public void updateHospitalDetails(UserRoleAssignment assignment, NurseHospitalDetailsDTO n) {
    if (n == null) {
      return;
    }
    NurseHospitalDetails det =
        nurseHospitalDetailsRepository
            .findById(assignment.getId())
            .orElseGet(
                () -> {
                  NurseHospitalDetails newDet = new NurseHospitalDetails();
                  newDet.setAssignment(assignment);
                  return newDet;
                });

    nurseMapper.updateEntity(n, det);
    nurseHospitalDetailsRepository.save(det);
    assignment.setNurseDetails(det);
  }
}
