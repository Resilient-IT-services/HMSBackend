package com.res.hms.user.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Role;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HospitalContext hospitalContext;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new ApiException("BUSINESS_RULE_VIOLATION", "Username already exists", HttpStatus.CONFLICT);
        }

        Set<Role> roles = request.roles().stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new ApiException("NOT_FOUND", "Role not found: " + code, HttpStatus.NOT_FOUND)))
                .collect(Collectors.toSet());

        Long hospitalId = hospitalContext.requireHospitalId();
        UserAccount userAccount = new UserAccount();
        userAccount.setHospitalId(hospitalId);
        userAccount.setUsername(request.username());
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setFullName(request.fullName());
        userAccount.setGender(request.gender());
        userAccount.setDob(request.dob());
        userAccount.setPhone(request.phone());
        userAccount.setRoles(roles);
        userAccount.setActive(true);
        userAccountRepository.save(userAccount);

        auditLogService.record(hospitalId, "user_account", userAccount.getId(), "CREATE", "Created user " + userAccount.getUsername());
        return toResponse(userAccount);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        Long hospitalId = hospitalContext.requireHospitalId();
        return userAccountRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(UserAccount userAccount) {
        return new UserResponse(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()),
                userAccount.isActive(),
                userAccount.getCreatedAt()
        );
    }
}
