package com.res.hms.user.service;

import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.RoleRepository;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserMembershipService {

  private final RoleRepository roleRepository;
  private final ResourceLookup resourceLookup;

  @Transactional
  public void updateUserMemberships(UserAccount user, UserRequestDTO request, boolean isSuperAdmin, UUID activeHospitalId) {
    if (request.getDetailedMemberships() == null) {
      return;
    }

    if (!isSuperAdmin && activeHospitalId != null) {
      manageHospitalAssignments(user, activeHospitalId, getRoleAssignmentsForHospital(request, activeHospitalId));
    } else {
      Set<UUID> hospitalsInRequest = request.getDetailedMemberships().stream()
          .map(UserFullProfileDTO.DetailedHospitalMembershipDTO::getHospitalId)
          .collect(Collectors.toSet());
      user.getAssignments().removeIf(a -> !hospitalsInRequest.contains(a.getHospitalId()));
      for (UserFullProfileDTO.DetailedHospitalMembershipDTO m : request.getDetailedMemberships()) {
        manageHospitalAssignments(user, m.getHospitalId(), m.getRoleAssignments());
      }
    }
  }

  public void assignRole(UserAccount user, UUID hospitalId, String roleCode, String departmentName) {
    Role role = resourceLookup.findRequired(RoleCode.valueOf(roleCode), roleRepository::findByCode, "Role");
    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(role);
    assignment.setDepartmentName(departmentName);
    user.getAssignments().add(assignment);
  }

  private List<UserFullProfileDTO.DetailedRoleAssignmentDTO> getRoleAssignmentsForHospital(UserRequestDTO request, UUID hospitalId) {
    return request.getDetailedMemberships().stream()
        .filter(m -> (hospitalId == null ? m.getHospitalId() == null : hospitalId.equals(m.getHospitalId())))
        .flatMap(m -> m.getRoleAssignments().stream())
        .collect(Collectors.toList());
  }

  private void manageHospitalAssignments(UserAccount user, UUID hospitalId, List<UserFullProfileDTO.DetailedRoleAssignmentDTO> requestedRoles) {
    if (requestedRoles == null) {
      return;
    }
    Set<RoleCode> currentRoleCodes = user.getAssignments().stream()
        .filter(a -> (hospitalId == null ? a.getHospitalId() == null : hospitalId.equals(a.getHospitalId())))
        .map(a -> a.getRole().getCode())
        .collect(Collectors.toSet());
    Set<RoleCode> requestedRoleCodes = requestedRoles.stream()
        .map(ra -> RoleCode.valueOf(ra.getRoleCode()))
        .collect(Collectors.toSet());
    user.getAssignments().removeIf(a -> (hospitalId == null ? a.getHospitalId() == null : hospitalId.equals(a.getHospitalId())) && !requestedRoleCodes.contains(a.getRole().getCode()));
    for (UserFullProfileDTO.DetailedRoleAssignmentDTO ra : requestedRoles) {
      RoleCode code = RoleCode.valueOf(ra.getRoleCode());
      if (!currentRoleCodes.contains(code)) {
        assignRole(user, hospitalId, ra.getRoleCode(), ra.getDepartmentName());
      } else {
        user.getAssignments().stream()
            .filter(a -> (hospitalId == null ? a.getHospitalId() == null : hospitalId.equals(a.getHospitalId())) && a.getRole().getCode() == code)
            .findFirst()
            .ifPresent(a -> {
              a.setDepartmentName(ra.getDepartmentName());
              a.setActive(ra.isActive());
            });
      }
    }
  }
}
