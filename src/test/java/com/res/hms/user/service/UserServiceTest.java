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
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Gender;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.AllocateRoleRequest;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.UserResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

  private final UUID hospitalId = UUID.nameUUIDFromBytes("hospital".getBytes());
  private final UUID userId = UUID.nameUUIDFromBytes("user".getBytes());

  @Test
  void createAutomaticallyCreatesDoctorEntry() {
    Role role = new Role();
    role.setCode(RoleCode.DOCTOR);

    when(userAccountRepository.existsByUsername("doctor1")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(resourceLookup.findRequired(eq(RoleCode.DOCTOR), any(), anyString())).thenReturn(role);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(doctorRepository.findByUserId(any())).thenReturn(Optional.empty());

    userService.create(
        new CreateUserRequest(
            "doctor1",
            "password",
            Set.of(RoleCode.DOCTOR),
            "Dr. John",
            Gender.MALE,
            LocalDate.now(),
            hospitalId,
            false,
            "123",
            "Cardiology",
            null,
            BigDecimal.valueOf(500),
            5));

    verify(doctorRepository).save(any(Doctor.class));
  }

  @Test
  void createAutomaticallyCreatesNurseEntry() {
    Role role = new Role();
    role.setCode(RoleCode.NURSE);

    when(userAccountRepository.existsByUsername("nurse1")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(resourceLookup.findRequired(eq(RoleCode.NURSE), any(), anyString())).thenReturn(role);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(nurseRepository.findByUserId(any())).thenReturn(Optional.empty());

    userService.create(
        new CreateUserRequest(
            "nurse1",
            "password",
            Set.of(RoleCode.NURSE),
            "Nurse Jane",
            Gender.FEMALE,
            LocalDate.now(),
            hospitalId,
            false,
            "123",
            null,
            "Emergency",
            BigDecimal.valueOf(500),
            5));

    verify(nurseRepository).save(any(Nurse.class));
  }

  @Test
  void allocateRolesAutomaticallyCreatesDoctorEntry() {
    Role role = new Role();
    role.setCode(RoleCode.DOCTOR);

    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setUsername("user1");

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);
    when(resourceLookup.findRequired(eq(userId), eq(hospitalId), any(), anyString()))
        .thenReturn(user);
    when(resourceLookup.findRequired(eq(hospitalId), any(), anyString()))
        .thenReturn(new Hospital());
    when(resourceLookup.findRequired(eq(RoleCode.DOCTOR), any(), anyString())).thenReturn(role);
    when(doctorRepository.findByUserId(eq(userId))).thenReturn(Optional.empty());

    userService.allocateRoles(
        new AllocateRoleRequest(
            userId,
            hospitalId,
            false,
            Set.of(RoleCode.DOCTOR),
            "Neurology",
            null,
            BigDecimal.valueOf(500),
            5));

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
                        hospitalId,
                        false,
                        "123",
                        null,
                        null,
                        BigDecimal.valueOf(500),
                        5)));

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    assertEquals("Username already exists", exception.getMessage());
  }

  @Test
  void createRejectsUnknownRole() {
    when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
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
                        hospitalId,
                        false,
                        "123",
                        null,
                        null,
                        BigDecimal.valueOf(500),
                        5)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void createPersistsUserAndAudits() {
    Role role = new Role();
    role.setCode(RoleCode.ADMIN);
    when(userAccountRepository.existsByUsername("admin")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);
    when(resourceLookup.findRequired(eq(RoleCode.ADMIN), any(), anyString())).thenReturn(role);
    when(passwordEncoder.encode("secret1")).thenReturn("encoded");
    when(userAccountRepository.save(any(UserAccount.class)))
        .thenAnswer(
            invocation -> {
              UserAccount userAccount = invocation.getArgument(0);
              userAccount.setId(userId);
              return userAccount;
            });
    when(userMapper.toResponse(any(UserAccount.class)))
        .thenAnswer(
            invocation -> {
              UserAccount user = invocation.getArgument(0);
              return new UserResponse(
                  user.getId(),
                  user.getUsername(),
                  user.getFullName(),
                  user.getGender(),
                  user.getDob(),
                  user.getPhone(),
                  List.of(
                      new HospitalMembershipResponse(
                          hospitalId, "Test", "TEST", user.getRoleCodes(), null, null)),
                  user.isActive(),
                  user.isDeleted(),
                  user.isSuperuser(),
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
                hospitalId,
                false,
                "123",
                null,
                null,
                BigDecimal.valueOf(500),
                5));

    assertEquals("admin", response.username());
    assertTrue(response.memberships().stream().anyMatch(m -> m.roles().contains(RoleCode.ADMIN)));
  }

  @Test
  void listReturnsHospitalUsers() {
    Role role = new Role();
    role.setCode(RoleCode.DOCTOR);

    UserAccount userAccount = new UserAccount();
    userAccount.setId(userId);
    userAccount.setUsername("doctor");
    userAccount.setActive(true);

    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setHospitalId(hospitalId);
    assignment.setRole(role);
    assignment.setUser(userAccount);
    userAccount.getAssignments().add(assignment);

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(userAccountRepository.findAllByHospitalId(hospitalId)).thenReturn(List.of(userAccount));
    when(userMapper.toResponse(any(UserAccount.class)))
        .thenAnswer(
            invocation -> {
              UserAccount user = invocation.getArgument(0);
              return new UserResponse(
                  user.getId(),
                  user.getUsername(),
                  user.getFullName(),
                  user.getGender(),
                  user.getDob(),
                  user.getPhone(),
                  List.of(
                      new HospitalMembershipResponse(
                          hospitalId, "Test Hospital", "TEST", user.getRoleCodes(), null, null)),
                  user.isActive(),
                  user.isDeleted(),
                  user.isSuperuser(),
                  null);
            });

    var response = userService.list(false);

    assertEquals(1, response.size());
    assertEquals("doctor", response.getFirst().username());
    assertTrue(
        response.getFirst().memberships().stream()
            .anyMatch(m -> m.roles().contains(RoleCode.DOCTOR)));
  }

  @Test
  void updateUpdatesUserProfile() {
    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setFullName("Old Name");

    when(resourceLookup.findRequired(eq(userId), any(), any())).thenReturn(user);
    when(userMapper.toResponse(any(UserAccount.class)))
        .thenAnswer(
            i -> {
              UserAccount u = i.getArgument(0);
              return new UserResponse(
                  u.getId(),
                  u.getUsername(),
                  u.getFullName(),
                  u.getGender(),
                  u.getDob(),
                  u.getPhone(),
                  List.of(),
                  u.isActive(),
                  u.isDeleted(),
                  u.isSuperuser(),
                  null);
            });

    var response =
        userService.update(
            userId,
            new com.res.hms.user.dto.UpdateUserRequest(
                "New Name", Gender.FEMALE, LocalDate.now(), "999", true));

    assertEquals("New Name", response.fullName());
    verify(userAccountRepository).save(user);
  }

  @Test
  void deleteSoftDeletesUser() {
    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setDeleted(false);

    when(resourceLookup.findRequired(eq(userId), any(), any())).thenReturn(user);

    userService.delete(userId);

    assertTrue(user.isDeleted());
    verify(userAccountRepository).save(user);
  }

  @Test
  void listIncludesDeletedWhenRequested() {
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(null);
    when(hospitalContext.isSuperAdmin()).thenReturn(true);
    when(userAccountRepository.findAll()).thenReturn(List.of());

    userService.list(true);

    verify(userAccountRepository).findAll();
  }
}
