package com.res.hms.user.service;

import com.res.hms.domain.LabTechProfile;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.LabTechProfileRepository;
import com.res.hms.user.dto.UserFullProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabTechProfileService {

  private final LabTechProfileRepository labTechProfileRepository;

  @Transactional
  public void updateProfile(UserAccount user, UserFullProfileDTO.LabTechInfo info) {
    if (info != null) {
      LabTechProfile profile = labTechProfileRepository.findById(user.getId())
          .orElseGet(() -> {
            LabTechProfile p = new LabTechProfile();
            p.setUser(user);
            return p;
          });
      if (info.getSpecialty() != null) {
        profile.setSpecialty(info.getSpecialty());
      }
      if (info.getLicenseNo() != null) {
        profile.setLicenseNo(info.getLicenseNo());
      }
      labTechProfileRepository.save(profile);
      user.setLabTechProfile(profile);
    }
  }
}
