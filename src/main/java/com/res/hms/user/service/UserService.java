package com.res.hms.user.service;

import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.AllocateRoleRequest;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.UserResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isCallerSuperAdmin = hospitalContext.isSuperAdmin();

    // If Super Admin, use hospitalId from request, otherwise use active context
    UUID targetHospitalId = isCallerSuperAdmin ? request.hospitalId() : activeHospitalId;

    validateCreateRequest(request, isCallerSuperAdmin, targetHospitalId);

    UserAccount userAccount = new UserAccount();
    userAccount.setUsername(request.username());
    userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
    userAccount.setFullName(request.fullName());
    userAccount.setGender(request.gender());
    userAccount.setDob(request.dob());
    userAccount.setPhone(request.phone());
    userAccount.setActive(true);

    if (isCallerSuperAdmin && request.isSuperuser() != null) {
      userAccount.setSuperuser(request.isSuperuser());
    }

    assignRoles(userAccount, request.roles(), targetHospitalId);
    userAccountRepository.save(userAccount);

    handleRoleSpecificEntries(
        userAccount,
        request.roles(),
        request.specialization(),
        request.department(),
        request.consultationFee(),
        request.followUpDays());

    return userMapper.toResponse(userAccount);
  }

  private void handleRoleSpecificEntries(
      UserAccount user,
      Set<RoleCode> roles,
      String specialization,
      String department,
      BigDecimal consultationFee,
      Integer followUpDays) {
    if (roles.contains(RoleCode.DOCTOR)) {
      Doctor doctor =
          doctorRepository
              .findByUserId(user.getId())
              .orElseGet(
                  () -> {
                    Doctor newDoc = new Doctor();
                    newDoc.setUser(user);
                    return newDoc;
                  });
      doctor.setSpecialization(specialization != null ? specialization : "General");
      doctor.setConsultationFee(consultationFee);
      doctor.setFollowUpDays(followUpDays);
      doctorRepository.save(doctor);
    }

    if (roles.contains(RoleCode.NURSE)) {
      Nurse nurse =
          nurseRepository
              .findByUserId(user.getId())
              .orElseGet(
                  () -> {
                    Nurse newNurse = new Nurse();
                    newNurse.setUser(user);
                    return newNurse;
                  });
      nurse.setDepartment(department != null ? department : "General");
      nurseRepository.save(nurse);
    }
  }

  private void assignRoles(UserAccount user, Set<RoleCode> roleCodes, UUID activeHospitalId) {
    for (RoleCode code : roleCodes) {
      Role role = resourceLookup.findRequired(code, roleRepository::findByCode, "Role: " + code);
      UserRoleAssignment assignment = new UserRoleAssignment();
      assignment.setUser(user);
      assignment.setHospitalId(activeHospitalId);
      assignment.setRole(role);
      user.getAssignments().add(assignment);
    }
  }

  public void validateCreateRequest(
      CreateUserRequest request, boolean isSuperAdmin, UUID hospitalId) {
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

  private void validateHospitalContext(boolean isSuperAdmin, UUID hospitalId, Set<RoleCode> roles) {
    if (!isSuperAdmin) {
      if (hospitalId == null) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
      }
    } else {
      if (hospitalId == null) {
        // Super admin can have no hospital context if assigning global permissions,
        // but our current model requires hospitalId for non-superuser assignments.
        // For now, we keep it simple: if hospitalId is null, it better be a superuser or have no
        // roles.
      }
    }
  }

  @Transactional
  @Auditable(entity = "user_account", action = "ALLOCATE")
  public UserResponse allocateRoles(AllocateRoleRequest request) {
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isCallerSuperAdmin = hospitalContext.isSuperAdmin();

    UserAccount user = resolveUser(request, isCallerSuperAdmin, activeHospitalId);
    UUID targetHospitalId = isCallerSuperAdmin ? request.hospitalId() : activeHospitalId;

    if (targetHospitalId != null) {
      resourceLookup.findRequired(targetHospitalId, hospitalRepository::findById, "Hospital");
    }

    user.getAssignments()
        .removeIf(
            a ->
                (targetHospitalId == null && a.getHospitalId() == null)
                    || (targetHospitalId != null && targetHospitalId.equals(a.getHospitalId())));

    if (isCallerSuperAdmin && request.isSuperuser() != null) {
      user.setSuperuser(request.isSuperuser());
    }

    assignRoles(user, request.roles(), targetHospitalId);
    userAccountRepository.save(user);

    handleRoleSpecificEntries(
        user,
        request.roles(),
        request.specialization(),
        request.department(),
        request.consultationFee(),
        request.followUpDays());

    return userMapper.toResponse(user);
  }

  private UserAccount resolveUser(
      AllocateRoleRequest request, boolean isSuperAdmin, UUID activeHospitalId) {
    if (isSuperAdmin) {
      return resourceLookup.findRequired(request.userId(), userAccountRepository::findById, "User");
    }
    if (activeHospitalId == null) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Hospital must be selected", HttpStatus.BAD_REQUEST);
    }
    return resourceLookup.findRequired(
        request.userId(), activeHospitalId, userAccountRepository::findByIdAndHospitalId, "User");
  }

  @Transactional(readOnly = true)
  public List<UserResponse> list(boolean includeDeleted) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    if (hospitalId == null && !hospitalContext.isSuperAdmin()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }
    List<UserAccount> users;
    if (hospitalId == null) {
      users =
          includeDeleted
              ? userAccountRepository.findAll()
              : userAccountRepository.findAllByDeletedFalse();
    } else {
      users = userAccountRepository.findAllByHospitalId(hospitalId);
    }
    return users.stream().map(userMapper::toResponse).toList();
  }

  @Transactional
  public UserResponse update(UUID id, com.res.hms.user.dto.UpdateUserRequest request) {
    UserAccount user = resourceLookup.findRequired(id, userAccountRepository::findById, "User");

    user.setFullName(request.fullName());
    user.setGender(request.gender());
    user.setDob(request.dob());
    user.setPhone(request.phone());
    user.setActive(request.active());
    userAccountRepository.save(user);
    return userMapper.toResponse(user);
  }

  @Transactional
  public void delete(UUID id) {
    UserAccount user = resourceLookup.findRequired(id, userAccountRepository::findById, "User");
    user.setDeleted(true);
    userAccountRepository.save(user);
  }
}
