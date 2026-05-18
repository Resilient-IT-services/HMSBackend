package com.res.hms.iam.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.CollectionMergeUtil;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.DepartmentRepository;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final com.res.hms.iam.application.service.DoctorScheduleValidator doctorScheduleValidator;
  private final UserMapper userMapper;
  private final HospitalContext hospitalContext;
  private final DepartmentRepository departmentRepository;
  private final HospitalRepository hospitalRepository;

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
      user.setEmail(dto.getEmail());
    }
    if (dto.getFullName() != null) {
      user.setFullName(dto.getFullName());
    }
    if (dto.getGender() != null) {
      user.setGender(dto.getGender());
    }
    if (dto.getDob() != null) {
      user.setDob(dto.getDob());
    }
    if (dto.getPhone() != null) {
      user.setPhone(dto.getPhone());
    }
      if (isSuper && dto.getSuperuser() != null) {
          user.setSuperuser(dto.getSuperuser());
      }
      if (dto.getActive() != null) {
          if (!dto.getActive() && Objects.equals(user.getId(), hospitalContext.currentUserIdOrNull())) {
              throw new ApiException("BUSINESS_RULE_VIOLATION", "Users cannot deactivate themselves", HttpStatus.BAD_REQUEST);
          }
          user.setActive(dto.getActive());
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
        user.getContracts(),
        incoming,
        UserContract::getId,
        UserDetailsDTO.RoleAssignmentDTO::getAssignmentId,
        raDto -> createRoleAssignment(user, raDto),
        this::updateRoleAssignment,
        ra -> {
          if (isSuper || Objects.equals(activeId, ra.getHospitalId())) {
            user.getContracts().remove(ra);
          }
        });
  }

  private UserContract createRoleAssignment(
      UserAccount user, UserDetailsDTO.RoleAssignmentDTO dto) {
    Role role =
        resourceLookup.findRequired(
            com.res.hms.iam.domain.RoleCode.valueOf(dto.getRoleCode()),
            roleRepository::findByCode,
            "Role");

    UserContract ra = new UserContract();
    ra.setUser(user);
    ra.setHospitalId(dto.getHospitalId());
    ra.setRole(role);
    
    if (dto.getHospitalId() != null) {
        Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));
            
        if (dto.getDepartments() != null && !dto.getDepartments().isEmpty()) {
            Set<String> hospitalDeptNames = hospital.getDepartments().stream()
                .map(Department::getName)
                .collect(Collectors.toSet());
                
            for (String deptName : dto.getDepartments()) {
                if (!hospitalDeptNames.contains(deptName)) {
                    throw new ApiException("VALIDATION_ERROR", 
                        "Department " + deptName + " is not linked to hospital " + hospital.getName(), 
                        HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    if (dto.getDepartments() != null) {
      ra.setDepartments(dto.getDepartments().stream()
          .map(name -> departmentRepository.findByName(name)
              .orElseThrow(() -> new ApiException("NOT_FOUND", "Department " + name + " not found in catalog", HttpStatus.BAD_REQUEST)))
          .collect(Collectors.toSet()));
    }
    ra.setActive(dto.getActive() != null ? dto.getActive() : true);
    return ra;
  }

  private void updateRoleAssignment(
      UserContract existing, UserDetailsDTO.RoleAssignmentDTO dto) {
      
    if (existing.getHospitalId() != null) {
        Hospital hospital = hospitalRepository.findById(existing.getHospitalId())
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));
            
        if (dto.getDepartments() != null && !dto.getDepartments().isEmpty()) {
            Set<String> hospitalDeptNames = hospital.getDepartments().stream()
                .map(Department::getName)
                .collect(Collectors.toSet());
                
            for (String deptName : dto.getDepartments()) {
                if (!hospitalDeptNames.contains(deptName)) {
                    throw new ApiException("VALIDATION_ERROR", 
                        "Department " + deptName + " is not linked to hospital " + hospital.getName(), 
                        HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    if (dto.getDepartments() != null) {
      existing.setDepartments(dto.getDepartments().stream()
          .map(name -> departmentRepository.findByName(name)
              .orElseThrow(() -> new ApiException("NOT_FOUND", "Department " + name + " not found in catalog", HttpStatus.BAD_REQUEST)))
          .collect(Collectors.toSet()));
    }
    if (dto.getActive() != null) {
      existing.setActive(dto.getActive());
    }
  }
}
