package com.res.hms.user.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private HospitalContext hospitalContext;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    @Test
    void createRejectsDuplicateUsername() {
        when(userAccountRepository.existsByUsername("admin")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                userService.create(new CreateUserRequest("admin", "secret1", RoleCode.ADMIN))
        );

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
    }

    @Test
    void createRejectsUnknownRole() {
        when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                userService.create(new CreateUserRequest("admin", "secret1", RoleCode.ADMIN))
        );

        assertEquals("NOT_FOUND", exception.getCode());
    }

    @Test
    void createPersistsUserAndAudits() {
        Role role = new Role();
        role.setCode(RoleCode.ADMIN);
        when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("secret1")).thenReturn("encoded");
        when(hospitalContext.requireHospitalId()).thenReturn(10L);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount userAccount = invocation.getArgument(0);
            userAccount.setId(99L);
            return userAccount;
        });

        var response = userService.create(new CreateUserRequest("admin", "secret1", RoleCode.ADMIN));

        assertEquals("admin", response.username());
        assertEquals(RoleCode.ADMIN, response.role());
        verify(auditLogService).record(10L, "user_account", 99L, "CREATE", "Created user admin");
    }

    @Test
    void listReturnsHospitalUsers() {
        Role role = new Role();
        role.setCode(RoleCode.DOCTOR);
        UserAccount userAccount = new UserAccount();
        userAccount.setId(5L);
        userAccount.setUsername("doctor");
        userAccount.setRole(role);
        userAccount.setActive(true);
        when(hospitalContext.requireHospitalId()).thenReturn(12L);
        when(userAccountRepository.findAllByHospitalIdOrderByCreatedAtDesc(12L)).thenReturn(List.of(userAccount));

        var response = userService.list();

        assertEquals(1, response.size());
        assertEquals("doctor", response.getFirst().username());
    }
}
