package com.res.hms.user.service;

import com.res.hms.domain.AdminHospitalDetails;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.hospital.repository.AdminHospitalDetailsRepository;
import com.res.hms.user.dto.AdminHospitalDetailsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.hospital.mapper.AdminMapper;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

  private final AdminHospitalDetailsRepository adminHospitalDetailsRepository;
  private final AdminMapper adminMapper;

  @Transactional
  public void updateHospitalDetails(UserRoleAssignment a, AdminHospitalDetailsDTO ad) {
    if (ad == null) {
      return;
    }
    AdminHospitalDetails det =
        adminHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  AdminHospitalDetails newDet = new AdminHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });

    adminMapper.updateEntity(ad, det);
    adminHospitalDetailsRepository.save(det);
    a.setAdminDetails(det);
  }
}
