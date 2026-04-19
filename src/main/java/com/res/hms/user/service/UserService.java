package com.res.hms.user.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.AllocateRoleRequest;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final HospitalContext hospitalContext;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse create(CreateUserRequest request) {

        boolean isSuperAdmin = hospitalContext.isSuperAdmin();
        Long activeHospitalId = hospitalContext.getHospitalIdOrNull();

        validateCreateRequest(request, isSuperAdmin, activeHospitalId);

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(request.username());
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setFullName(request.fullName());
        userAccount.setGender(request.gender());
        userAccount.setDob(request.dob());
        userAccount.setPhone(request.phone());
        userAccount.setActive(true);
        Set<Role> roles = request.roles().stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new ApiException("NOT_FOUND", "Role not found: " + code, HttpStatus.NOT_FOUND)))
                .collect(Collectors.toSet());

        for (Role role : roles) {
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(userAccount);
            // SUPER_ADMIN role is always global (hospitalId = null)
            assignment.setHospitalId(role.getCode() == RoleCode.SUPER_ADMIN ? null : activeHospitalId);
            assignment.setRole(role);
            userAccount.getAssignments().add(assignment);
        }

        userAccountRepository.save(userAccount);

        auditLogService.record(activeHospitalId, "user_account", userAccount.getId(), "CREATE", "Created user " + userAccount.getUsername());
        return toResponse(userAccount);
    }


    public void validateCreateRequest(CreateUserRequest request, boolean isSuperAdmin, Long hospitalId) {

        if (request.roles() == null || request.roles().isEmpty()) {
            throw new ApiException("VALIDATION_ERROR", "At least one role is required", HttpStatus.BAD_REQUEST);
        }

        if (!isSuperAdmin && hospitalId == null) {
            throw new ApiException("BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
        }

        if (!isSuperAdmin && request.roles().contains(RoleCode.SUPER_ADMIN)) {
            throw new ApiException("FORBIDDEN", "Only Super Admin can create Super Admins", HttpStatus.FORBIDDEN);
        }

        if (isSuperAdmin && hospitalId == null && !request.roles().contains(RoleCode.SUPER_ADMIN)) {
            throw new ApiException("BUSINESS_RULE_VIOLATION", "Please select a hospital first or assign a global role", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public UserResponse allocateRoles(AllocateRoleRequest request) {
        boolean isCallerSuperAdmin = hospitalContext.isSuperAdmin();
        Long activeHospitalId = hospitalContext.getHospitalIdOrNull();

        UserAccount user;
        Long targetHospitalId;

        if (isCallerSuperAdmin) {
            user = userAccountRepository.findById(request.userId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));
            targetHospitalId = request.hospitalId();
        } else {
            if (activeHospitalId == null) {
                throw new ApiException("BUSINESS_RULE_VIOLATION", "Hospital must be selected", HttpStatus.BAD_REQUEST);
            }
            targetHospitalId = activeHospitalId;
            // Strict Isolation: Can only find users ALREADY in this hospital
            user = userAccountRepository.findByIdAndHospitalId(request.userId(), targetHospitalId)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found in your hospital context", HttpStatus.NOT_FOUND));

            if (request.roles().contains(RoleCode.SUPER_ADMIN)) {
                throw new ApiException("FORBIDDEN", "Only Super Admin can allocate Super Admin role", HttpStatus.FORBIDDEN);
            }
        }

        if (targetHospitalId != null && !hospitalRepository.existsById(targetHospitalId)) {
            throw new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND);
        }

        // Remove existing assignments for this user in this hospital context
        user.getAssignments().removeIf(a ->
                (targetHospitalId == null && a.getHospitalId() == null) ||
                        (targetHospitalId != null && targetHospitalId.equals(a.getHospitalId()))
        );

        // Add new assignments
        for (RoleCode code : request.roles()) {
            Role role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Role not found: " + code, HttpStatus.NOT_FOUND));

            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(user);
            assignment.setHospitalId(targetHospitalId);
            assignment.setRole(role);
            user.getAssignments().add(assignment);
        }

        userAccountRepository.save(user);
        auditLogService.record(targetHospitalId, "user_account", user.getId(), "ALLOCATE", "Allocated roles in hospital context: " + targetHospitalId);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        Long hospitalId = hospitalContext.getHospitalIdOrNull();
        boolean isSuperAdmin = hospitalContext.isSuperAdmin();

        if (hospitalId == null) {
            if (isSuperAdmin) {
                return userAccountRepository.findAll().stream()
                        .map(this::toResponse)
                        .toList();
            } else {
                throw new ApiException("BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
            }
        }

        return userAccountRepository.findAllByHospitalId(hospitalId).stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(UserAccount userAccount) {
        return new UserResponse(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getRoleCodes(),
                userAccount.isActive(),
                userAccount.getCreatedAt()
        );
    }
}
