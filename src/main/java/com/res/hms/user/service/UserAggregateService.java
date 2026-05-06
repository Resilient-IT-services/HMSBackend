package com.res.hms.user.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.CollectionMergeUtil;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Role;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.user.repository.RoleRepository;
import com.res.hms.user.repository.UserAccountRepository;
import com.res.hms.user.dto.UserDetailsDTO;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAggregateService {

  private final UserAccountRepository userAccountRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final ResourceLookup resourceLookup;
  private final UserRoleUpdateService userRoleUpdateService;
  private final UserValidationService userValidationService;
  private final com.res.hms.doctor.service.DoctorScheduleValidator doctorScheduleValidator;
  private final UserMapper userMapper;
  private final HospitalContext hospitalContext;

  @Transactional
  public UserDetailsDTO saveUserAggregate(UserDetailsDTO dto) {
    log.info("Starting aggregate save for user: {}", dto.getUsername());
    doctorScheduleValidator.validateOverlaps(dto);
    UUID activeId = hospitalContext.getHospitalIdOrNull();
    boolean isSuper = hospitalContext.isSuperAdmin();

    UserAccount user;
    if (dto.getId() == null) {
      validateCreate(dto);
      user = createBaseUser(dto);
    } else {
      user = findUser(dto.getId(), activeId, isSuper);
    }

    updateBaseUser(user, dto, isSuper);
    userValidationService.validateProfileUniqueness(dto, user.getId());
    mergeMemberships(user, dto, isSuper, activeId);

    userAccountRepository.saveAndFlush(user);
    userRoleUpdateService.handleProfileUpdate(user, dto, isSuper, activeId);

    userAccountRepository.flush();
    return userMapper.toDetailsDTO(user);
  }

  private UserAccount findUser(UUID id, UUID activeId, boolean isSuper) {
    if (isSuper) {
      return resourceLookup.findRequired(id, userAccountRepository::findById, "User");
    }
    return resourceLookup.findRequired(
        id, activeId, userAccountRepository::findByIdAndHospitalId, "User");
  }

  private void validateCreate(UserDetailsDTO dto) {
    if (userAccountRepository.existsByUsername(dto.getUsername())) {
      throw new ApiException("ALREADY_EXISTS", "Username already taken", HttpStatus.CONFLICT);
    }
    if (dto.getEmail() != null && userAccountRepository.existsByEmail(dto.getEmail())) {
      throw new ApiException("ALREADY_EXISTS", "Email already registered", HttpStatus.CONFLICT);
    }
    if ((dto.getMemberships() == null || dto.getMemberships().isEmpty())
        && (dto.getSuperuser() == null || !dto.getSuperuser())) {
      throw new ApiException(
          "VALIDATION_ERROR", "Membership required for non-superusers", HttpStatus.BAD_REQUEST);
    }
  }

  private UserAccount createBaseUser(UserDetailsDTO dto) {
    UserAccount user = new UserAccount();
    user.setUsername(dto.getUsername());
    user.setPasswordHash(
        passwordEncoder.encode(dto.getPassword() != null ? dto.getPassword() : "Pass@123"));
    user.setActive(true);
    return user;
  }

  private void updateBaseUser(UserAccount user, UserDetailsDTO dto, boolean isSuper) {
    if (dto.getEmail() != null) {
      if (user.getId() != null
          && userAccountRepository.existsByEmailAndIdNot(dto.getEmail(), user.getId())) {
        throw new ApiException("CONFLICT", "Email already exists", HttpStatus.CONFLICT);
      }
    }
    userMapper.updateEntity(dto, user);
    if (isSuper && dto.getSuperuser() != null) {
      user.setSuperuser(dto.getSuperuser());
    }
  }

  private void mergeMemberships(
      UserAccount user, UserDetailsDTO dto, boolean isSuper, UUID activeId) {
    if (dto.getMemberships() == null) {
      return;
    }

    java.util.List<UserDetailsDTO.RoleAssignmentDTO> incoming =
        dto.getMemberships().stream()
            .filter(m -> isSuper || Objects.equals(activeId, m.getHospitalId()))
            .flatMap(
                m ->
                    m.getRoleAssignments().stream().peek(ra -> ra.setHospitalId(m.getHospitalId())))
            .toList();

    CollectionMergeUtil.merge(
        user.getAssignments(),
        incoming,
        UserRoleAssignment::getId,
        UserDetailsDTO.RoleAssignmentDTO::getAssignmentId,
        raDto -> createRoleAssignment(user, raDto),
        (existing, raDto) -> userMapper.updateEntity(raDto, existing),
        ra -> {
          if (isSuper || Objects.equals(activeId, ra.getHospitalId())) {
            user.getAssignments().remove(ra);
          }
        });
  }

  private UserRoleAssignment createRoleAssignment(
      UserAccount user, UserDetailsDTO.RoleAssignmentDTO dto) {
    Role role =
        resourceLookup.findRequired(
            com.res.hms.domain.RoleCode.valueOf(dto.getRoleCode()),
            roleRepository::findByCode,
            "Role");

    UserRoleAssignment ra = new UserRoleAssignment();
    ra.setUser(user);
    ra.setHospitalId(dto.getHospitalId());
    ra.setRole(role);
    userMapper.updateEntity(dto, ra);
    if (dto.getActive() != null) {
      ra.setActive(dto.getActive());
    } else {
      ra.setActive(true);
    }
    return ra;
  }
}
