package com.res.hms.iam.application.service;

import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.AdminStatsDTO;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

  private final UserAccountRepository userRepository;
  private final HospitalRepository hospitalRepository;
  private final UserContractRepository assignmentRepository;

  @Transactional(readOnly = true)
  public AdminStatsDTO getAdminStats() {
    return AdminStatsDTO.builder()
        .totalUsers(userRepository.count())
        .activeUsers(userRepository.countByActive(true))
        .totalHospitals(hospitalRepository.countByDeleted(false))
        .totalDoctors(
            assignmentRepository.countDistinctUsersByRoleCodeAndActive(RoleCode.DOCTOR, true))
        .build();
  }
}
