package com.res.hms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Gender;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.AllocateRoleRequest;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.UserResponse;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Nurse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private NurseRepository nurseRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private HospitalContext hospitalContext;
  @Mock private UserMapper userMapper;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private UserService userService;

  @Test
  void createAutomaticallyCreatesDoctorEntry() {
    Role role = new Role();
    role.setCode(RoleCode.DOCTOR);

    when(userAccountRepository.existsByUsername("doctor1")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(10L);
    when(resourceLookup.findRequired(eq(RoleCode.DOCTOR), any(), anyString())).thenReturn(role);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(doctorRepository.findByUserIdAndHospitalId(any(), eq(10L))).thenReturn(Optional.empty());

    userService.create(
        new CreateUserRequest(
            "doctor1",
            "password",
            Set.of(RoleCode.DOCTOR),
            "Dr. John",
            Gender.MALE,
            LocalDate.now(),
            10L,
            "123",
            "Cardiology",
            null));

    verify(doctorRepository).save(any(Doctor.class));
  }

  @Test
  void createAutomaticallyCreatesNurseEntry() {
    Role role = new Role();
    role.setCode(RoleCode.NURSE);

    when(userAccountRepository.existsByUsername("nurse1")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(10L);
    when(resourceLookup.findRequired(eq(RoleCode.NURSE), any(), anyString())).thenReturn(role);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(nurseRepository.findByUserIdAndHospitalId(any(), eq(10L))).thenReturn(Optional.empty());

    userService.create(
        new CreateUserRequest(
            "nurse1",
            "password",
            Set.of(RoleCode.NURSE),
            "Nurse Jane",
            Gender.FEMALE,
            LocalDate.now(),
            10L,
            "123",
            null,
            "Emergency"));

    verify(nurseRepository).save(any(Nurse.class));
  }

  @Test
  void allocateRolesAutomaticallyCreatesDoctorEntry() {
    Role role = new Role();
    role.setCode(RoleCode.DOCTOR);
    
    UserAccount user = new UserAccount();
    user.setId(5L);
    user.setUsername("user1");

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(10L);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);
    when(resourceLookup.findRequired(eq(5L), eq(10L), any(), anyString())).thenReturn(user);
    when(resourceLookup.findRequired(eq(10L), any(), anyString())).thenReturn(new Hospital());
    when(resourceLookup.findRequired(eq(RoleCode.DOCTOR), any(), anyString())).thenReturn(role);
    when(doctorRepository.findByUserIdAndHospitalId(eq(5L), eq(10L))).thenReturn(Optional.empty());

    userService.allocateRoles(
        new AllocateRoleRequest(
            5L,
            10L,
            Set.of(RoleCode.DOCTOR),
            "Neurology",
            null));

    verify(doctorRepository).save(any(Doctor.class));
  }

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
                        "123",
                        null,
                        null)));

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    assertEquals("Username already exists", exception.getMessage());
  }

  @Test
  void createRejectsUnknownRole() {
    when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(10L);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);
    when(resourceLookup.findRequired(eq(RoleCode.ADMIN), any(), anyString()))
        .thenThrow(new ApiException("NOT_FOUND", "Role not found", HttpStatus.NOT_FOUND));

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
                        "123",
                        null,
                        null)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void createPersistsUserAndAudits() {
    Role role = new Role();
    role.setCode(RoleCode.ADMIN);
    when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(10L);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);
    when(resourceLookup.findRequired(eq(RoleCode.ADMIN), any(), anyString())).thenReturn(role);
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
                  user.getId(),
                  user.getUsername(),
                  List.of(
                      new HospitalMembershipResponse(10L, "Test", "TEST", user.getRoleCodes(), null, null)),
                  user.isActive(),
                  null);
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
                "123",
                null,
                null));

    assertEquals("admin", response.username());
    assertTrue(
        response.memberships().stream().anyMatch(m -> m.roles().contains(RoleCode.ADMIN)));
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
                  user.getId(),
                  user.getUsername(),
                  List.of(
                      new HospitalMembershipResponse(
                          12L, "Test Hospital", "TEST", user.getRoleCodes(), null, null)),
                  user.isActive(),
                  null);
            });

    var response = userService.list();

    assertEquals(1, response.size());
    assertEquals("doctor", response.getFirst().username());
    assertTrue(
        response.getFirst().memberships().stream()
            .anyMatch(m -> m.roles().contains(RoleCode.DOCTOR)));
  }
}
