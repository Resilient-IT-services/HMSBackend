package com.res.hms.iam.application.service;

import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.application.dto.UserDetailsDTO;
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
  public void updateUserMemberships(
      UserAccount user, UserDetailsDTO request, boolean isSuperAdmin, UUID activeHospitalId) {
    if (request.getMemberships() == null) {
      return;
    }

    if (!isSuperAdmin && activeHospitalId != null) {
      manageHospitalAssignments(
          user, activeHospitalId, getRoleAssignmentsForHospital(request, activeHospitalId));
    } else {
      Set<UUID> hospitalsInRequest =
          request.getMemberships().stream()
              .map(UserDetailsDTO.HospitalMembershipDTO::getHospitalId)
              .collect(Collectors.toSet());
      user.getAssignments().removeIf(a -> !hospitalsInRequest.contains(a.getHospitalId()));
      for (UserDetailsDTO.HospitalMembershipDTO m : request.getMemberships()) {
        manageHospitalAssignments(user, m.getHospitalId(), m.getRoleAssignments());
      }
    }
  }

  public void assignRole(
      UserAccount user, UUID hospitalId, String roleCode, String departmentName) {
    Role role =
        resourceLookup.findRequired(RoleCode.valueOf(roleCode), roleRepository::findByCode, "Role");
    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(role);
    assignment.setDepartmentName(departmentName);
    user.getAssignments().add(assignment);
  }

  private List<UserDetailsDTO.RoleAssignmentDTO> getRoleAssignmentsForHospital(
      UserDetailsDTO request, UUID hospitalId) {
    return request.getMemberships().stream()
        .filter(
            m ->
                (hospitalId == null
                    ? m.getHospitalId() == null
                    : hospitalId.equals(m.getHospitalId())))
        .flatMap(m -> m.getRoleAssignments().stream())
        .collect(Collectors.toList());
  }

  private void manageHospitalAssignments(
      UserAccount user, UUID hospitalId, List<UserDetailsDTO.RoleAssignmentDTO> requestedRoles) {
    if (requestedRoles == null) {
      return;
    }
    Set<RoleCode> currentRoleCodes =
        user.getAssignments().stream()
            .filter(
                a ->
                    (hospitalId == null
                        ? a.getHospitalId() == null
                        : hospitalId.equals(a.getHospitalId())))
            .map(a -> a.getRole().getCode())
            .collect(Collectors.toSet());
    Set<RoleCode> requestedRoleCodes =
        requestedRoles.stream()
            .map(ra -> RoleCode.valueOf(ra.getRoleCode()))
            .collect(Collectors.toSet());
    user.getAssignments()
        .removeIf(
            a ->
                (hospitalId == null
                        ? a.getHospitalId() == null
                        : hospitalId.equals(a.getHospitalId()))
                    && !requestedRoleCodes.contains(a.getRole().getCode()));
    for (UserDetailsDTO.RoleAssignmentDTO ra : requestedRoles) {
      RoleCode code = RoleCode.valueOf(ra.getRoleCode());
      if (!currentRoleCodes.contains(code)) {
        assignRole(user, hospitalId, ra.getRoleCode(), ra.getDepartmentName());
      } else {
        user.getAssignments().stream()
            .filter(
                a ->
                    (hospitalId == null
                            ? a.getHospitalId() == null
                            : hospitalId.equals(a.getHospitalId()))
                        && a.getRole().getCode() == code)
            .findFirst()
            .ifPresent(
                a -> {
                  a.setDepartmentName(ra.getDepartmentName());
                  a.setActive(ra.getActive());
                });
      }
    }
  }
}
