package com.res.hms.iam.application.service;

import com.res.hms.iam.domain.AdminHospitalDetails;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.AdminHospitalDetailsRepository;
import com.res.hms.iam.application.dto.AdminHospitalDetailsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

  private final AdminHospitalDetailsRepository adminHospitalDetailsRepository;

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
    if (ad.getOfficeLocation() != null) {
      det.setOfficeLocation(ad.getOfficeLocation());
    }
    det.setDepartmentHead(ad.isDepartmentHead());
    adminHospitalDetailsRepository.save(det);
    a.setAdminDetails(det);
  }
}
