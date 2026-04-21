package com.res.hms.user.service;

import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Nurse;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.AllocateRoleRequest;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.UserResponse;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AvoidInstantiatingObjectsInLoops"})
public class UserService {

  private final UserAccountRepository userAccountRepository;
  private final RoleRepository roleRepository;
  private final HospitalRepository hospitalRepository;
  private final DoctorRepository doctorRepository;
  private final NurseRepository nurseRepository;
  private final PasswordEncoder passwordEncoder;
  private final HospitalContext hospitalContext;
  private final UserMapper userMapper;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "user_account", action = "CREATE")
  public UserResponse create(CreateUserRequest request) {
    Long activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isSuperAdmin = hospitalContext.isSuperAdmin();
    
    // If Super Admin, use hospitalId from request, otherwise use active context
    Long targetHospitalId = isSuperAdmin ? request.hospitalId() : activeHospitalId;

    validateCreateRequest(request, isSuperAdmin, targetHospitalId);

    UserAccount userAccount = new UserAccount();
    userAccount.setUsername(request.username());
    userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
    userAccount.setFullName(request.fullName());
    userAccount.setGender(request.gender());
    userAccount.setDob(request.dob());
    userAccount.setPhone(request.phone());
    userAccount.setActive(true);

    assignRoles(userAccount, request.roles(), targetHospitalId);
    userAccountRepository.save(userAccount);

    handleRoleSpecificEntries(userAccount, request.roles(), request.specialization(), request.department());

    return userMapper.toResponse(userAccount);
  }

  private void handleRoleSpecificEntries(UserAccount user, Set<RoleCode> roles, String specialization, String department) {
    if (roles.contains(RoleCode.DOCTOR)) {
      Doctor doctor = doctorRepository.findByUserId(user.getId())
          .orElseGet(() -> {
            Doctor newDoc = new Doctor();
            newDoc.setUser(user);
            return newDoc;
          });
      doctor.setSpecialization(specialization != null ? specialization : "General");
      doctorRepository.save(doctor);
    }

    if (roles.contains(RoleCode.NURSE)) {
      Nurse nurse = nurseRepository.findByUserId(user.getId())
          .orElseGet(() -> {
            Nurse newNurse = new Nurse();
            newNurse.setUser(user);
            return newNurse;
          });
      nurse.setDepartment(department != null ? department : "General");
      nurseRepository.save(nurse);
    }
  }

  private void assignRoles(UserAccount user, Set<RoleCode> roleCodes, Long activeHospitalId) {
    for (RoleCode code : roleCodes) {
      Role role = resourceLookup.findRequired(code, roleRepository::findByCode, "Role: " + code);
      UserRoleAssignment assignment = new UserRoleAssignment();
      assignment.setUser(user);
      assignment.setHospitalId(code == RoleCode.SUPER_ADMIN ? null : activeHospitalId);
      assignment.setRole(role);
      user.getAssignments().add(assignment);
    }
  }

  public void validateCreateRequest(
      CreateUserRequest request, boolean isSuperAdmin, Long hospitalId) {
    validateUsernameUnique(request.username());
    validateRolesPresent(request.roles());
    validateHospitalContext(isSuperAdmin, hospitalId, request.roles());
  }

  private void validateUsernameUnique(String username) {
    if (userAccountRepository.existsByUsername(username)) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Username already exists", HttpStatus.CONFLICT);
    }
  }

  private void validateRolesPresent(Set<RoleCode> roles) {
    if (roles == null || roles.isEmpty()) {
      throw new ApiException(
          "VALIDATION_ERROR", "At least one role is required", HttpStatus.BAD_REQUEST);
    }
  }

  private void validateHospitalContext(boolean isSuperAdmin, Long hospitalId, Set<RoleCode> roles) {
    if (!isSuperAdmin) {
      if (hospitalId == null) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
      }
      if (roles.contains(RoleCode.SUPER_ADMIN)) {
        throw new ApiException(
            "FORBIDDEN", "Only Super Admin can create Super Admins", HttpStatus.FORBIDDEN);
      }
    } else {
      if (hospitalId == null && !roles.contains(RoleCode.SUPER_ADMIN)) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Please select a hospital first or assign a global role",
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  @Transactional
  @Auditable(entity = "user_account", action = "ALLOCATE")
  public UserResponse allocateRoles(AllocateRoleRequest request) {
    Long activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isCallerSuperAdmin = hospitalContext.isSuperAdmin();

    UserAccount user = resolveUser(request, isCallerSuperAdmin, activeHospitalId);
    Long targetHospitalId = isCallerSuperAdmin ? request.hospitalId() : activeHospitalId;

    if (targetHospitalId != null) {
      resourceLookup.findRequired(targetHospitalId, hospitalRepository::findById, "Hospital");
    }

    user.getAssignments()
        .removeIf(
            a ->
                (targetHospitalId == null && a.getHospitalId() == null)
                    || (targetHospitalId != null && targetHospitalId.equals(a.getHospitalId())));

    assignRoles(user, request.roles(), targetHospitalId);
    userAccountRepository.save(user);

    handleRoleSpecificEntries(user, request.roles(), request.specialization(), request.department());

    return userMapper.toResponse(user);
  }

  private UserAccount resolveUser(
      AllocateRoleRequest request, boolean isSuperAdmin, Long activeHospitalId) {
    if (isSuperAdmin) {
      return resourceLookup.findRequired(request.userId(), userAccountRepository::findById, "User");
    }
    if (activeHospitalId == null) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Hospital must be selected", HttpStatus.BAD_REQUEST);
    }
    if (request.roles().contains(RoleCode.SUPER_ADMIN)) {
      throw new ApiException(
          "FORBIDDEN", "Only Super Admin can allocate Super Admin role", HttpStatus.FORBIDDEN);
    }
    return resourceLookup.findRequired(
        request.userId(), activeHospitalId, userAccountRepository::findByIdAndHospitalId, "User");
  }

  @Transactional(readOnly = true)
  public List<UserResponse> list() {
    Long hospitalId = hospitalContext.getHospitalIdOrNull();
    if (hospitalId == null && !hospitalContext.isSuperAdmin()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }
    List<UserAccount> users =
        (hospitalId == null)
            ? userAccountRepository.findAll()
            : userAccountRepository.findAllByHospitalId(hospitalId);
    return users.stream().map(userMapper::toResponse).toList();
  }
}
