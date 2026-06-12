package com.res.hms.iam.application.service;

import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.domain.LabAdminProfile;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.LabAdminProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabAdminProfileService {

  private final LabAdminProfileRepository labAdminProfileRepository;

  @Transactional
  public void updateProfile(UserAccount user, UserDetailsDTO.LabAdminInfo info) {
    if (info != null) {
      LabAdminProfile profile =
          labAdminProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    LabAdminProfile p = new LabAdminProfile();
                    p.setUser(user);
                    return p;
                  });
      if (info.getSpecialty() != null) {
        profile.setSpecialty(info.getSpecialty());
      }
      if (info.getLicenseNo() != null) {
        profile.setLicenseNo(info.getLicenseNo());
      }
      labAdminProfileRepository.save(profile);
      user.setLabAdminProfile(profile);
    }
  }
}
