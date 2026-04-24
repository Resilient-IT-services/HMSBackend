package com.res.hms.user.service;

import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final NurseRepository nurseRepository;
  private final PasswordEncoder passwordEncoder;
  private final HospitalContext hospitalContext;
  private final UserMapper userMapper;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "user_account", action = "CREATE")
  public UserSummaryDTO createUser(UserRequestDTO request) {
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isCallerSuperAdmin = hospitalContext.isSuperAdmin();

    validateCreateRequest(request);

    UserAccount user = new UserAccount();
    user.setUsername(request.getUsername());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setFullName(request.getFullName());
    user.setGender(request.getGender());
    user.setDob(request.getDob());
    user.setPhone(request.getPhone());
    user.setActive(true);

    if (isCallerSuperAdmin && request.getSuperuser() != null) {
        user.setSuperuser(request.getSuperuser());
    }

    if (request.getMemberships() != null) {
        for (UserRequestDTO.MembershipRequestDTO memRequest : request.getMemberships()) {
            UUID hospitalId = isCallerSuperAdmin ? memRequest.getHospitalId() : activeHospitalId;
            if (hospitalId != null) {
                assignRolesFromDTO(user, memRequest, hospitalId);
            }
        }
    }

    userAccountRepository.save(user);
    
    // Handle role specific profile data for all assigned hospitals
    Set<UUID> hospitalIds = user.getAssignments().stream()
            .map(UserRoleAssignment::getHospitalId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    
    if (hospitalIds.isEmpty()) {
        handleProfileUpdate(user, request, null);
    } else {
        for (UUID hid : hospitalIds) {
            handleProfileUpdate(user, request, hid);
        }
    }

    return userMapper.toSummaryDTO(user);
  }

  @Transactional
  @Auditable(entity = "user_account", action = "UPDATE")
  public UserSummaryDTO updateUser(UUID userId, UserRequestDTO request) {
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isSuperAdmin = hospitalContext.isSuperAdmin();

    UserAccount user;
    if (isSuperAdmin) {
        user = resourceLookup.findRequired(userId, userAccountRepository::findById, "User");
    } else {
        if (activeHospitalId == null) {
            throw new ApiException("AUTH_ERROR", "Hospital context required for update", HttpStatus.FORBIDDEN);
        }
        user = resourceLookup.findRequired(userId, activeHospitalId, userAccountRepository::findByIdAndHospitalId, "User");
    }

    // 1. Update basic user info
    if (request.getFullName() != null) user.setFullName(request.getFullName());
    if (request.getGender() != null) user.setGender(request.getGender());
    if (request.getDob() != null) user.setDob(request.getDob());
    if (request.getPhone() != null) user.setPhone(request.getPhone());

    if (isSuperAdmin && request.getSuperuser() != null) {
        user.setSuperuser(request.getSuperuser());
    }

    // 2. Scoped Update Logic for Memberships
    if (request.getMemberships() != null) {
        if (!isSuperAdmin) {
            // Remove existing assignments ONLY for the active hospital
            user.getAssignments().removeIf(a -> activeHospitalId.equals(a.getHospitalId()));
            
            // Add new assignments for the active hospital from the request
            request.getMemberships().stream()
                .filter(m -> activeHospitalId.equals(m.getHospitalId()))
                .forEach(m -> assignRolesFromDTO(user, m, activeHospitalId));
        } else {
            // SuperAdmin can update everything.
            user.getAssignments().clear();
            request.getMemberships().forEach(m -> assignRolesFromDTO(user, m, m.getHospitalId()));
        }
    }

    userAccountRepository.save(user);
    
    // For update, we only update profile details for the active context
    handleProfileUpdate(user, request, activeHospitalId);

    return userMapper.toSummaryDTO(user);
  }

  private void assignRolesFromDTO(UserAccount user, UserRequestDTO.MembershipRequestDTO memRequest, UUID hospitalId) {
      if (memRequest.getRoles() != null) {
          for (RoleCode code : memRequest.getRoles()) {
              Role role = resourceLookup.findRequired(code, roleRepository::findByCode, "Role");
              UserRoleAssignment assignment = new UserRoleAssignment();
              assignment.setUser(user);
              assignment.setHospitalId(hospitalId);
              assignment.setRole(role);
              user.getAssignments().add(assignment);
          }
      }
  }

  private void handleProfileUpdate(UserAccount user, UserRequestDTO request, UUID activeHospitalId) {
      Set<RoleCode> allRoles = user.getRoleCodes();
      
      if (allRoles.contains(RoleCode.DOCTOR)) {
           Doctor doctor = doctorRepository.findByUserId(user.getId())
                  .orElseGet(() -> {
                      Doctor d = new Doctor();
                      d.setUser(user);
                      return d;
                  });
          if (request.getSpecialization() != null) doctor.setSpecialization(request.getSpecialization());
          doctorRepository.save(doctor);

          // Update hospital-specific doctor details
          if (activeHospitalId != null) {
              user.getAssignments().stream()
                  .filter(a -> activeHospitalId.equals(a.getHospitalId()) && a.getRole().getCode() == RoleCode.DOCTOR)
                  .findFirst()
                  .ifPresent(a -> {
                      DoctorHospitalDetails det = doctorHospitalDetailsRepository.findById(a.getId())
                          .orElseGet(() -> {
                              DoctorHospitalDetails d = new DoctorHospitalDetails();
                              d.setAssignment(a);
                              return d;
                          });
                      if (request.getConsultationFee() != null) det.setConsultationFee(request.getConsultationFee());
                      if (request.getFollowUpDays() != null) det.setFollowUpDays(request.getFollowUpDays());
                      det.setUpdatedAt(LocalDateTime.now());
                      doctorHospitalDetailsRepository.save(det);
                  });
          }
      }

      if (allRoles.contains(RoleCode.NURSE)) {
          Nurse nurse = nurseRepository.findByUserId(user.getId())
                  .orElseGet(() -> {
                      Nurse n = new Nurse();
                      n.setUser(user);
                      return n;
                  });
          if (request.getDepartment() != null) nurse.setDepartment(request.getDepartment());
          nurseRepository.save(nurse);
      }
  }

  @Transactional(readOnly = true)
  public List<UserSummaryDTO> listUsers(boolean includeDeleted) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isSuperAdmin = hospitalContext.isSuperAdmin();

    if (hospitalId == null && !isSuperAdmin) {
      throw new ApiException("BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }

    List<UserAccount> users;
    if (isSuperAdmin && hospitalId == null) {
        users = includeDeleted ? userAccountRepository.findAll() : userAccountRepository.findAllByDeletedFalse();
    } else {
        users = userAccountRepository.findAllByHospitalId(hospitalId);
    }

    return users.stream().map(userMapper::toSummaryDTO).toList();
  }

  @Transactional(readOnly = true)
  public UserFullProfileDTO getFullProfile(UUID userId) {
      UserAccount user = resourceLookup.findRequired(userId, userAccountRepository::findById, "User");
      return userMapper.toFullProfileDTO(user);
  }

  @Transactional(readOnly = true)
  public UserSummaryDTO getUser(UUID userId) {
      UserAccount user = resourceLookup.findRequired(userId, userAccountRepository::findById, "User");
      return userMapper.toSummaryDTO(user);
  }

  private void validateCreateRequest(UserRequestDTO request) {
      if (userAccountRepository.existsByUsername(request.getUsername())) {
          throw new ApiException("BUSINESS_RULE_VIOLATION", "Username already exists", HttpStatus.CONFLICT);
      }
      if (request.getMemberships() == null || request.getMemberships().isEmpty()) {
          if (request.getSuperuser() == null || !request.getSuperuser()) {
              throw new ApiException("VALIDATION_ERROR", "At least one hospital membership is required for non-superusers", HttpStatus.BAD_REQUEST);
          }
      }
  }

  @Transactional
  public void deleteUser(UUID id) {
    UserAccount user = resourceLookup.findRequired(id, userAccountRepository::findById, "User");
    user.setDeleted(true);
    userAccountRepository.save(user);
  }
}
