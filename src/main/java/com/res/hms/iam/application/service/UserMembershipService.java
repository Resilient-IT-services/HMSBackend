package com.res.hms.iam.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.util.CollectionMergeUtil;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.DepartmentRepository;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserMembershipService {

  private static final String NOT_FOUND_CODE = "NOT_FOUND";
  private static final String DEPARTMENT_TEXT = "Department ";

  private final RoleRepository roleRepository;
  private final ResourceLookup resourceLookup;
  private final UserRoleUpdateService userRoleUpdateService;
  private final DepartmentRepository departmentRepository;
  private final HospitalRepository hospitalRepository;

  @Transactional
  public void mergeMemberships(
      UserAccount user, UserDetailsDTO dto, boolean isSuper, UUID activeId) {
    if (dto.getMemberships() == null) {
      return;
    }

    List<UserDetailsDTO.RoleAssignmentDTO> incoming =
        dto.getMemberships().stream()
            .filter(m -> isSuper || Objects.equals(activeId, m.getHospitalId()))
            .flatMap(
                m ->
                    m.getRoleAssignments().stream().map(ra -> normalizeRoleAssignment(user, m, ra)))
            .toList();

    CollectionMergeUtil.merge(
        user.getContracts(),
        incoming,
        UserContract::getId,
        UserDetailsDTO.RoleAssignmentDTO::getAssignmentId,
        raDto -> createRoleAssignment(user, raDto),
        this::updateRoleAssignment,
        ra -> {
          if (isSuper || Objects.equals(activeId, ra.getHospitalId())) {
            userRoleUpdateService.handleAssignmentDeletion(ra);
            user.getContracts().remove(ra);
          }
        });
  }

  private UserDetailsDTO.RoleAssignmentDTO normalizeRoleAssignment(
      UserAccount user,
      UserDetailsDTO.HospitalMembershipDTO membership,
      UserDetailsDTO.RoleAssignmentDTO roleAssignment) {
    roleAssignment.setHospitalId(membership.getHospitalId());
    if (roleAssignment.getAssignmentId() == null) {
      findExistingAssignmentId(user, membership.getHospitalId(), roleAssignment.getRoleCode())
          .ifPresent(roleAssignment::setAssignmentId);
    }
    return roleAssignment;
  }

  private Optional<UUID> findExistingAssignmentId(
      UserAccount user, UUID hospitalId, String roleCode) {
    if (roleCode == null) {
      return Optional.empty();
    }
    return user.getContracts().stream()
        .filter(
            contract ->
                Objects.equals(contract.getHospitalId(), hospitalId)
                    && contract.getRole() != null
                    && roleCode.equals(contract.getRole().getCode().name()))
        .map(UserContract::getId)
        .findFirst();
  }

  private UserContract createRoleAssignment(
      UserAccount user, UserDetailsDTO.RoleAssignmentDTO dto) {
    Role role =
        resourceLookup.findRequired(
            RoleCode.valueOf(dto.getRoleCode()), roleRepository::findByCode, "Role");

    UserContract ra = new UserContract();
    ra.setUser(user);
    ra.setHospitalId(dto.getHospitalId());
    ra.setRole(role);

    validateAndSetDepartments(ra, dto.getHospitalId(), dto.getDepartments());

    ra.setActive(dto.getActive() == null || dto.getActive());
    return ra;
  }

  private void updateRoleAssignment(UserContract existing, UserDetailsDTO.RoleAssignmentDTO dto) {
    validateAndSetDepartments(existing, existing.getHospitalId(), dto.getDepartments());
    if (dto.getActive() != null) {
      existing.setActive(dto.getActive());
    }
  }

  private void validateAndSetDepartments(
      UserContract ra, UUID hospitalId, List<String> departmentNames) {
    if (hospitalId != null && departmentNames != null && !departmentNames.isEmpty()) {
      Hospital hospital =
          hospitalRepository
              .findById(hospitalId)
              .orElseThrow(
                  () ->
                      new ApiException(NOT_FOUND_CODE, "Hospital not found", HttpStatus.NOT_FOUND));

      Set<String> hospitalDeptNames =
          hospital.getDepartments().stream().map(Department::getName).collect(Collectors.toSet());

      for (String deptName : departmentNames) {
        if (!hospitalDeptNames.contains(deptName)) {
          throw new ApiException(
              "VALIDATION_ERROR",
              DEPARTMENT_TEXT + deptName + " is not linked to hospital " + hospital.getName(),
              HttpStatus.BAD_REQUEST);
        }
      }
    }

    if (departmentNames != null) {
      ra.setDepartments(
          departmentNames.stream()
              .map(
                  name ->
                      departmentRepository
                          .findByName(name)
                          .orElseThrow(
                              () ->
                                  new ApiException(
                                      NOT_FOUND_CODE,
                                      DEPARTMENT_TEXT + name + " not found in catalog",
                                      HttpStatus.BAD_REQUEST)))
              .collect(Collectors.toSet()));
    }
  }
}
