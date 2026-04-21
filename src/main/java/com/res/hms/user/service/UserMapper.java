package com.res.hms.user.service;

import com.res.hms.domain.Hospital;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.UserResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

  private final HospitalRepository hospitalRepository;
  private final DoctorRepository doctorRepository;
  private final NurseRepository nurseRepository;

  public UserResponse toResponse(UserAccount userAccount) {
    Map<Long, Set<RoleCode>> groupedRoles = new HashMap<>();
    if (userAccount.getAssignments() != null) {
      for (UserRoleAssignment assignment : userAccount.getAssignments()) {
        groupedRoles
            .computeIfAbsent(assignment.getHospitalId(), k -> new HashSet<>())
            .add(assignment.getRole().getCode());
      }
    }

    List<HospitalMembershipResponse> memberships = new ArrayList<>();
    for (Map.Entry<Long, Set<RoleCode>> entry : groupedRoles.entrySet()) {
      Long hospitalId = entry.getKey();
      Set<RoleCode> roles = entry.getValue();
      String specialization = null;
      String department = null;

      if (hospitalId != null) {
        if (roles.contains(RoleCode.DOCTOR)) {
          specialization = doctorRepository.findByUserIdAndHospitalId(userAccount.getId(), hospitalId)
              .map(d -> d.getSpecialization())
              .orElse(null);
        }
        if (roles.contains(RoleCode.NURSE)) {
          department = nurseRepository.findByUserIdAndHospitalId(userAccount.getId(), hospitalId)
              .map(n -> n.getDepartment())
              .orElse(null);
        }

        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
        memberships.add(
            new HospitalMembershipResponse(
                hospital.getId(), hospital.getName(), hospital.getCode(), roles, specialization, department));
      } else {
        memberships.add(
            new HospitalMembershipResponse(null, "Global Access", "GLOBAL", roles, null, null));
      }
    }

    return new UserResponse(
        userAccount.getId(),
        userAccount.getUsername(),
        memberships,
        userAccount.isActive(),
        userAccount.getCreatedAt());
  }
}
