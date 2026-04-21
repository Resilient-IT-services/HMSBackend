package com.res.hms.user.service;

import com.res.hms.domain.Hospital;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.UserResponse;
import java.util.ArrayList;
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

  public UserResponse toResponse(UserAccount userAccount) {
    Map<Long, Set<RoleCode>> groupedRoles =
        userAccount.getAssignments().stream()
            .collect(
                Collectors.groupingBy(
                    UserRoleAssignment::getHospitalId,
                    Collectors.mapping(a -> a.getRole().getCode(), Collectors.toSet())));

    List<HospitalMembershipResponse> memberships = new ArrayList<>();
    for (Map.Entry<Long, Set<RoleCode>> entry : groupedRoles.entrySet()) {
      if (entry.getKey() != null) {
        Hospital hospital = hospitalRepository.findById(entry.getKey()).orElseThrow();
        memberships.add(
            new HospitalMembershipResponse(
                hospital.getId(), hospital.getName(), hospital.getCode(), entry.getValue()));
      } else {
        memberships.add(
            new HospitalMembershipResponse(null, "Global Access", "GLOBAL", entry.getValue()));
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
