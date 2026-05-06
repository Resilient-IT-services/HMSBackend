package com.res.hms.user.service;

import com.res.hms.domain.LabTechProfile;
import com.res.hms.domain.UserAccount;
import com.res.hms.staff.repository.LabTechProfileRepository;
import com.res.hms.user.dto.UserDetailsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.staff.mapper.StaffInternalMapper;

@Service
@RequiredArgsConstructor
public class LabTechProfileService {

  private final LabTechProfileRepository labTechProfileRepository;
  private final StaffInternalMapper staffInternalMapper;

  @Transactional
  public void updateProfile(UserAccount user, UserDetailsDTO.LabTechInfo info) {
    if (info != null) {
      LabTechProfile profile =
          labTechProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    LabTechProfile p = new LabTechProfile();
                    p.setUser(user);
                    return p;
                  });

      staffInternalMapper.updateEntity(info, profile);
      labTechProfileRepository.save(profile);
      user.setLabTechProfile(profile);
    }
  }
}
