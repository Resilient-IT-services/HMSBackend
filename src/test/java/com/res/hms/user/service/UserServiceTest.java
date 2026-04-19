package com.res.hms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Gender;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.UserResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private HospitalContext hospitalContext;
  @Mock private UserMapper userMapper;

  @InjectMocks private UserService userService;

  @Test
  void createRejectsDuplicateUsername() {
    when(userAccountRepository.existsByUsername("admin")).thenReturn(true);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                userService.create(
                    new CreateUserRequest(
                        "admin",
                        "secret1",
                        Set.of(RoleCode.ADMIN),
                        "Admin",
                        Gender.MALE,
                        LocalDate.now(),
                        10L,
                        "123")));

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
    assertEquals("Username already exists", exception.getMessage());
  }

  @Test
  void createRejectsUnknownRole() {
    when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(10L);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);
    when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                userService.create(
                    new CreateUserRequest(
                        "admin",
                        "secret1",
                        Set.of(RoleCode.ADMIN),
                        "Admin",
                        Gender.MALE,
                        LocalDate.now(),
                        10L,
                        "123")));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void createPersistsUserAndAudits() {
    Role role = new Role();
    role.setCode(RoleCode.ADMIN);
    when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(10L);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);
    when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
    when(passwordEncoder.encode("secret1")).thenReturn("encoded");
    when(userAccountRepository.save(any(UserAccount.class)))
        .thenAnswer(
            invocation -> {
              UserAccount userAccount = invocation.getArgument(0);
              userAccount.setId(99L);
              return userAccount;
            });
    when(userMapper.toResponse(any(UserAccount.class)))
        .thenAnswer(
            invocation -> {
              UserAccount user = invocation.getArgument(0);
              return new UserResponse(
                  user.getId(), user.getUsername(), user.getRoleCodes(), user.isActive(), null);
            });

    var response =
        userService.create(
            new CreateUserRequest(
                "admin",
                "secret1",
                Set.of(RoleCode.ADMIN),
                "Admin User",
                Gender.MALE,
                LocalDate.now(),
                10L,
                "123"));

    assertEquals("admin", response.username());
    assertTrue(response.roles().contains(RoleCode.ADMIN));
  }

  @Test
  void listReturnsHospitalUsers() {
    Role role = new Role();
    role.setCode(RoleCode.DOCTOR);

    UserAccount userAccount = new UserAccount();
    userAccount.setId(5L);
    userAccount.setUsername("doctor");
    userAccount.setActive(true);

    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setHospitalId(12L);
    assignment.setRole(role);
    assignment.setUser(userAccount);
    userAccount.getAssignments().add(assignment);

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(12L);
    when(userAccountRepository.findAllByHospitalId(12L)).thenReturn(List.of(userAccount));
    when(userMapper.toResponse(any(UserAccount.class)))
        .thenAnswer(
            invocation -> {
              UserAccount user = invocation.getArgument(0);
              return new UserResponse(
                  user.getId(), user.getUsername(), user.getRoleCodes(), user.isActive(), null);
            });

    var response = userService.list();

    assertEquals(1, response.size());
    assertEquals("doctor", response.getFirst().username());
    assertTrue(response.getFirst().roles().contains(RoleCode.DOCTOR));
  }
}
