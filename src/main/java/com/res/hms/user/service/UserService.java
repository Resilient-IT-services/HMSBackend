package com.res.hms.user.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.LabTechProfileRepository;
import com.res.hms.repository.PharmacistProfileRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
  private final DoctorRepository doctorRepository;
  private final NurseRepository nurseRepository;
  private final PharmacistProfileRepository pharmacistProfileRepository;
  private final LabTechProfileRepository labTechProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final HospitalContext hospitalContext;
  private final UserMapper userMapper;
  private final ResourceLookup resourceLookup;
  private final UserRoleUpdateService userRoleUpdateService;

  @Transactional
  @Auditable(entity = "user_account", action = "CREATE")
  public UserFullProfileDTO createUser(UserRequestDTO request) {
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isCallerSuperAdmin = hospitalContext.isSuperAdmin();

    validateCreateRequest(request);

    UserAccount user = new UserAccount();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setFullName(request.getFullName());
    user.setGender(request.getGender());
    user.setDob(request.getDob());
    user.setPhone(request.getPhone());
    user.setActive(true);

    if (isCallerSuperAdmin && request.getSuperuser() != null) {
      user.setSuperuser(request.getSuperuser());
    }

    if (request.getDetailedMemberships() != null) {
      for (UserFullProfileDTO.DetailedHospitalMembershipDTO memRequest :
          request.getDetailedMemberships()) {
        UUID hospitalId = memRequest.getHospitalId();
        if (!isCallerSuperAdmin && activeHospitalId != null) {
          hospitalId = activeHospitalId;
        }
        if (hospitalId != null && memRequest.getRoleAssignments() != null) {
          for (UserFullProfileDTO.DetailedRoleAssignmentDTO ra : memRequest.getRoleAssignments()) {
            assignRole(user, hospitalId, ra.getRoleCode(), ra.getDepartmentName());
          }
        }
      }
    }

    userAccountRepository.saveAndFlush(user);
    userRoleUpdateService.handleProfileUpdate(user, request);
    userAccountRepository.flush();

    return userMapper.toFullProfileDTO(userAccountRepository.findById(user.getId())
        .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found after save", HttpStatus.INTERNAL_SERVER_ERROR)));
  }

  @Transactional
  @Auditable(entity = "user_account", action = "UPDATE")
  public UserFullProfileDTO updateUser(UUID userId, UserRequestDTO request) {
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isSuperAdmin = hospitalContext.isSuperAdmin();

    UserAccount user = isSuperAdmin 
        ? resourceLookup.findRequired(userId, userAccountRepository::findById, "User")
        : resourceLookup.findRequired(userId, activeHospitalId, userAccountRepository::findByIdAndHospitalId, "User");

    updateUserDetails(user, request, isSuperAdmin);
    
    if (request.getEmail() != null && userAccountRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
      throw new ApiException("BUSINESS_RULE_VIOLATION", "Email already exists", HttpStatus.CONFLICT);
    }
    validateProfileUniqueness(request, userId);

    updateUserMemberships(user, request, isSuperAdmin, activeHospitalId);

    userAccountRepository.saveAndFlush(user);
    userRoleUpdateService.handleProfileUpdate(user, request);
    userAccountRepository.flush();

    return userMapper.toFullProfileDTO(userAccountRepository.findById(userId)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found after save", HttpStatus.INTERNAL_SERVER_ERROR)));
  }

  private void updateUserDetails(UserAccount user, UserRequestDTO request, boolean isSuperAdmin) {
    if (request.getFullName() != null) {
      user.setFullName(request.getFullName());
    }
    if (request.getGender() != null) {
      user.setGender(request.getGender());
    }
    if (request.getDob() != null) {
      user.setDob(request.getDob());
    }
    if (request.getPhone() != null) {
      user.setPhone(request.getPhone());
    }
    if (request.getEmail() != null) {
      user.setEmail(request.getEmail());
    }
    if (isSuperAdmin && request.getSuperuser() != null) {
      user.setSuperuser(request.getSuperuser());
    }
  }

  private void updateUserMemberships(UserAccount user, UserRequestDTO request, boolean isSuperAdmin, UUID activeHospitalId) {
    if (request.getDetailedMemberships() == null) {
      return;
    }

    if (!isSuperAdmin) {
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

  private List<UserFullProfileDTO.DetailedRoleAssignmentDTO> getRoleAssignmentsForHospital(UserRequestDTO request, UUID hospitalId) {
    return request.getDetailedMemberships().stream()
        .filter(m -> hospitalId.equals(m.getHospitalId()))
        .flatMap(m -> m.getRoleAssignments().stream())
        .collect(Collectors.toList());
  }

  private void manageHospitalAssignments(UserAccount user, UUID hospitalId, List<UserFullProfileDTO.DetailedRoleAssignmentDTO> requestedRoles) {
    if (requestedRoles == null) {
      return;
    }
    Set<RoleCode> currentRoleCodes = user.getAssignments().stream()
        .filter(a -> hospitalId.equals(a.getHospitalId()))
        .map(a -> a.getRole().getCode())
        .collect(Collectors.toSet());
    Set<RoleCode> requestedRoleCodes = requestedRoles.stream()
        .map(ra -> RoleCode.valueOf(ra.getRoleCode()))
        .collect(Collectors.toSet());
    user.getAssignments().removeIf(a -> hospitalId.equals(a.getHospitalId()) && !requestedRoleCodes.contains(a.getRole().getCode()));
    for (UserFullProfileDTO.DetailedRoleAssignmentDTO ra : requestedRoles) {
      RoleCode code = RoleCode.valueOf(ra.getRoleCode());
      if (!currentRoleCodes.contains(code)) {
        assignRole(user, hospitalId, ra.getRoleCode(), ra.getDepartmentName());
      } else {
        user.getAssignments().stream()
            .filter(a -> hospitalId.equals(a.getHospitalId()) && a.getRole().getCode() == code)
            .findFirst()
            .ifPresent(a -> {
              a.setDepartmentName(ra.getDepartmentName());
              a.setActive(ra.isActive());
            });
      }
    }
  }

  private void assignRole(UserAccount user, UUID hospitalId, String roleCode, String departmentName) {
    Role role = resourceLookup.findRequired(RoleCode.valueOf(roleCode), roleRepository::findByCode, "Role");
    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(role);
    assignment.setDepartmentName(departmentName);
    user.getAssignments().add(assignment);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummaryDTO> listUsers(int page, int size, String search, String role, Boolean active, boolean includeDeleted) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    if (hospitalId == null && !hospitalContext.isSuperAdmin()) {
      throw new ApiException("BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }
    RoleCode roleCode = (role != null && !role.isBlank()) ? RoleCode.valueOf(role.toUpperCase()) : null;
    String searchPattern = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;
    return PageResponse.from(userAccountRepository.searchUsers(hospitalId, active, includeDeleted ? null : false, roleCode, searchPattern, PageRequest.of(page, size)).map(userMapper::toSummaryDTO));
  }

  @Transactional(readOnly = true)
  public UserFullProfileDTO getFullProfile(UUID userId) {
    return userMapper.toFullProfileDTO(resourceLookup.findRequired(userId, userAccountRepository::findById, "User"));
  }

  private void validateCreateRequest(UserRequestDTO request) {
    if (userAccountRepository.existsByUsername(request.getUsername())) {
      throw new ApiException("BUSINESS_RULE_VIOLATION", "Username already exists", HttpStatus.CONFLICT);
    }
    if (request.getEmail() != null && userAccountRepository.existsByEmail(request.getEmail())) {
      throw new ApiException("BUSINESS_RULE_VIOLATION", "Email already exists", HttpStatus.CONFLICT);
    }
    validateProfileUniqueness(request, null);
    if ((request.getDetailedMemberships() == null || request.getDetailedMemberships().isEmpty()) && (request.getSuperuser() == null || !request.getSuperuser())) {
      throw new ApiException("VALIDATION_ERROR", "At least one hospital membership is required for non-superusers", HttpStatus.BAD_REQUEST);
    }
  }

  @Transactional
  public void deleteUser(UUID id) {
    UserAccount user = resourceLookup.findRequired(id, userAccountRepository::findById, "User");
    user.setDeleted(true);
    userAccountRepository.save(user);
  }

  private void validateProfileUniqueness(UserRequestDTO request, UUID userId) {
    if (request.getDoctorInfo() != null && request.getDoctorInfo().getLicenseNumber() != null) {
      String ln = request.getDoctorInfo().getLicenseNumber();
      boolean exists = (userId == null) ? doctorRepository.existsByLicenseNumber(ln) : doctorRepository.existsByLicenseNumberAndUserIdNot(ln, userId);
      if (exists) {
        throw new ApiException("BUSINESS_RULE_VIOLATION", "Doctor license number already exists", HttpStatus.CONFLICT);
      }
    }
    if (request.getNurseInfo() != null && request.getNurseInfo().getNursingLicenseNo() != null) {
      String ln = request.getNurseInfo().getNursingLicenseNo();
      boolean exists = (userId == null) ? nurseRepository.existsByNursingLicenseNo(ln) : nurseRepository.existsByNursingLicenseNoAndUserIdNot(ln, userId);
      if (exists) {
        throw new ApiException("BUSINESS_RULE_VIOLATION", "Nurse license number already exists", HttpStatus.CONFLICT);
      }
    }
    if (request.getPharmacistInfo() != null && request.getPharmacistInfo().getPciRegNo() != null) {
      String reg = request.getPharmacistInfo().getPciRegNo();
      boolean exists = (userId == null) ? pharmacistProfileRepository.existsByPciRegNo(reg) : pharmacistProfileRepository.existsByPciRegNoAndUserIdNot(reg, userId);
      if (exists) {
        throw new ApiException("BUSINESS_RULE_VIOLATION", "Pharmacist PCI registration number already exists", HttpStatus.CONFLICT);
      }
    }
    if (request.getLabTechInfo() != null && request.getLabTechInfo().getLicenseNo() != null) {
      String ln = request.getLabTechInfo().getLicenseNo();
      boolean exists = (userId == null) ? labTechProfileRepository.existsByLicenseNo(ln) : labTechProfileRepository.existsByLicenseNoAndUserIdNot(ln, userId);
      if (exists) {
        throw new ApiException("BUSINESS_RULE_VIOLATION", "Lab Tech license number already exists", HttpStatus.CONFLICT);
      }
    }
  }
}
