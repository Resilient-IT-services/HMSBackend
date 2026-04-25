package com.res.hms.user.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import java.util.UUID;
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
  private final PasswordEncoder passwordEncoder;
  private final HospitalContext hospitalContext;
  private final UserMapper userMapper;
  private final ResourceLookup resourceLookup;
  private final UserRoleUpdateService userRoleUpdateService;
  private final UserMembershipService userMembershipService;
  private final UserValidationService userValidationService;

  @Transactional
  @Auditable(entity = "user_account", action = "CREATE")
  public UserFullProfileDTO createUser(UserRequestDTO request) {
    UUID activeId = hospitalContext.getHospitalIdOrNull();
    boolean isSuper = hospitalContext.isSuperAdmin();

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

    if (isSuper && request.getSuperuser() != null) {
      user.setSuperuser(request.getSuperuser());
    }

    if (request.getDetailedMemberships() != null) {
      for (UserFullProfileDTO.DetailedHospitalMembershipDTO m : request.getDetailedMemberships()) {
        UUID hId = m.getHospitalId();
        if (!isSuper && activeId != null) {
          hId = activeId;
        }
        if (hId != null && m.getRoleAssignments() != null) {
          for (UserFullProfileDTO.DetailedRoleAssignmentDTO ra : m.getRoleAssignments()) {
            userMembershipService.assignRole(user, hId, ra.getRoleCode(), ra.getDepartmentName());
          }
        }
      }
    }

    userAccountRepository.saveAndFlush(user);
    userRoleUpdateService.handleProfileUpdate(user, request, isSuper, activeId);
    userAccountRepository.flush();

    return userMapper.toFullProfileDTO(userAccountRepository.findById(user.getId())
        .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found after save", HttpStatus.INTERNAL_SERVER_ERROR)));
  }

  @Transactional
  @Auditable(entity = "user_account", action = "UPDATE")
  public UserFullProfileDTO updateUser(UUID userId, UserRequestDTO request) {
    UUID activeId = hospitalContext.getHospitalIdOrNull();
    boolean isSuper = hospitalContext.isSuperAdmin();

    UserAccount user = isSuper 
        ? resourceLookup.findRequired(userId, userAccountRepository::findById, "User")
        : resourceLookup.findRequired(userId, activeId, userAccountRepository::findByIdAndHospitalId, "User");

    updateUserDetails(user, request, isSuper);
    
    if (request.getEmail() != null && userAccountRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
      throw new ApiException("BUSINESS_RULE_VIOLATION", "Email already exists", HttpStatus.CONFLICT);
    }
    userValidationService.validateProfileUniqueness(request, userId);

    userMembershipService.updateUserMemberships(user, request, isSuper, activeId);

    userAccountRepository.saveAndFlush(user);
    userRoleUpdateService.handleProfileUpdate(user, request, isSuper, activeId);
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
    userValidationService.validateProfileUniqueness(request, null);
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
}
