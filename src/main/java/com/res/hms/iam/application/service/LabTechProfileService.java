package com.res.hms.iam.application.service;

import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.domain.LabTechProfile;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.LabTechProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabTechProfileService {

  private final LabTechProfileRepository labTechProfileRepository;

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
