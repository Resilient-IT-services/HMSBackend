package com.res.hms.iam.application.service;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.application.dto.AdminResetPasswordRequest;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.dto.UserSummaryDTO;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AvoidInstantiatingObjectsInLoops"})
public class UserService {

  private final UserAccountRepository userAccountRepository;
  private final HospitalContext hospitalContext;
  private final UserMapper userMapper;
  private final ResourceLookup resourceLookup;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void resetPassword(UUID userId, AdminResetPasswordRequest request) {
    UserAccount user;
    if (hospitalContext.isSuperAdmin()) {
      user = resourceLookup.findRequired(userId, userAccountRepository::findById, "User");
    } else {
      user =
          resourceLookup.findRequired(
              userId,
              hospitalContext.requireHospitalId(),
              userAccountRepository::findByIdAndHospitalId,
              "User");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userAccountRepository.save(user);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummaryDTO> listUsers(
      int page, int size, String search, String role, Boolean active, boolean includeDeleted) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    if (hospitalId == null && !hospitalContext.isSuperAdmin()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }
    RoleCode roleCode =
        (role != null && !role.isBlank()) ? RoleCode.valueOf(role.toUpperCase()) : null;
    String searchPattern =
        (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;
    return PageResponse.from(
        userAccountRepository
            .searchUsers(
                hospitalId,
                active,
                includeDeleted ? null : false,
                roleCode,
                searchPattern,
                PageRequest.of(page, size))
            .map(userMapper::toSummaryDTO));
  }

  @Transactional(readOnly = true)
  public UserDetailsDTO getFullProfile(UUID userId) {
    return userMapper.toDetailsDTO(
        resourceLookup.findRequired(userId, userAccountRepository::findById, "User"));
  }

  @Transactional
  public void deleteUser(UUID id) {
    UserAccount user = resourceLookup.findRequired(id, userAccountRepository::findById, "User");
    user.setDeleted(true);
    userAccountRepository.save(user);
  }
}
