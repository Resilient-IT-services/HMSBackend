package com.res.hms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.Gender;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.AdminHospitalDetailsRepository;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.LabTechProfileRepository;
import com.res.hms.repository.NurseHospitalDetailsRepository;
import com.res.hms.repository.PharmacistHospitalDetailsRepository;
import com.res.hms.repository.PharmacistProfileRepository;
import com.res.hms.repository.ReceptionistHospitalDetailsRepository;
import com.res.hms.repository.ReceptionistProfileRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  @Mock private AdminHospitalDetailsRepository adminHospitalDetailsRepository;
  @Mock private NurseHospitalDetailsRepository nurseHospitalDetailsRepository;
  @Mock private PharmacistHospitalDetailsRepository pharmacistHospitalDetailsRepository;
  @Mock private ReceptionistHospitalDetailsRepository receptionistHospitalDetailsRepository;
  @Mock private NurseRepository nurseRepository;
  @Mock private PharmacistProfileRepository pharmacistProfileRepository;
  @Mock private LabTechProfileRepository labTechProfileRepository;
  @Mock private ReceptionistProfileRepository receptionistProfileRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private HospitalContext hospitalContext;
  @Mock private UserMapper userMapper;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private UserService userService;

  private final UUID hospitalId = UUID.nameUUIDFromBytes("hospital".getBytes());
  private final UUID userId = UUID.nameUUIDFromBytes("user".getBytes());

  @Test
  void listUsersReturnsPaginatedResults() {
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);

    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setFullName("Test User");

    when(userAccountRepository.searchUsers(
            eq(hospitalId),
            eq(true),
            eq(false),
            eq(RoleCode.DOCTOR),
            eq("%test%"),
            any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(user)));
    when(userMapper.toSummaryDTO(user))
        .thenReturn(UserSummaryDTO.builder().id(userId).fullName("Test User").build());

    var response = userService.listUsers(0, 10, "test", RoleCode.DOCTOR.name(), true, false);

    assertEquals(1, response.totalElements());
    assertEquals("Test User", response.content().getFirst().getFullName());
  }

  @Test
  void createUserAutomaticallyCreatesDoctorEntry() {
    Role role = new Role();
    role.setCode(RoleCode.DOCTOR);

    when(userAccountRepository.existsByUsername("doctor1")).thenReturn(false);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(resourceLookup.findRequired(eq(RoleCode.DOCTOR), any(), anyString())).thenReturn(role);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(doctorRepository.findByUserId(any())).thenReturn(Optional.empty());

    when(userAccountRepository.saveAndFlush(any(UserAccount.class)))
        .thenAnswer(
            i -> {
              UserAccount u = i.getArgument(0);
              u.setId(userId);
              return u;
            });
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(new UserAccount()));
    when(userMapper.toFullProfileDTO(any()))
        .thenReturn(UserFullProfileDTO.builder().id(userId).build());

    userService.createUser(
        UserRequestDTO.builder()
            .username("doctor1")
            .password("password")
            .fullName("Dr. John")
            .gender(Gender.MALE)
            .dob(LocalDate.now())
            .doctorInfo(
                UserFullProfileDTO.DoctorInfo.builder().specialization("Cardiology").build())
            .detailedMemberships(
                List.of(
                    UserFullProfileDTO.DetailedHospitalMembershipDTO.builder()
                        .hospitalId(hospitalId)
                        .roleAssignments(
                            List.of(
                                UserFullProfileDTO.DetailedRoleAssignmentDTO.builder()
                                    .roleCode("DOCTOR")
                                    .doctorDetails(
                                        DoctorHospitalDetailsDTO.builder()
                                            .consultationFee(BigDecimal.valueOf(500))
                                            .followUpDays(5)
                                            .build())
                                    .build()))
                        .build()))
            .build());

    verify(doctorRepository).save(any(Doctor.class));
    verify(doctorHospitalDetailsRepository).save(any(DoctorHospitalDetails.class));
  }

  @Test
  void updateUserScopedLogic() {
    UserAccount user = new UserAccount();
    user.setId(userId);

    Role adminRole = new Role();
    adminRole.setCode(RoleCode.ADMIN);

    UserRoleAssignment existingHosp1 = new UserRoleAssignment();
    existingHosp1.setId(UUID.randomUUID());
    existingHosp1.setHospitalId(hospitalId);
    existingHosp1.setRole(adminRole);
    user.getAssignments().add(existingHosp1);

    UUID otherHospId = UUID.randomUUID();
    UserRoleAssignment existingHosp2 = new UserRoleAssignment();
    existingHosp2.setId(UUID.randomUUID());
    existingHosp2.setHospitalId(otherHospId);
    existingHosp2.setRole(adminRole);
    user.getAssignments().add(existingHosp2);

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(hospitalContext.isSuperAdmin()).thenReturn(false);

    when(resourceLookup.findRequired(eq(userId), eq(hospitalId), any(), anyString()))
        .thenReturn(user);
    when(resourceLookup.findRequired(eq(RoleCode.DOCTOR), any(), anyString()))
        .thenReturn(new Role());

    lenient().when(doctorRepository.findByUserId(any())).thenReturn(Optional.empty());
    lenient().when(nurseRepository.findByUserId(any())).thenReturn(Optional.empty());

    when(userAccountRepository.saveAndFlush(any(UserAccount.class))).thenReturn(user);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toFullProfileDTO(any()))
        .thenReturn(UserFullProfileDTO.builder().id(userId).build());

    userService.updateUser(
        userId,
        UserRequestDTO.builder()
            .username("user1")
            .fullName("Updated")
            .gender(Gender.MALE)
            .dob(LocalDate.now())
            .detailedMemberships(
                List.of(
                    UserFullProfileDTO.DetailedHospitalMembershipDTO.builder()
                        .hospitalId(hospitalId)
                        .roleAssignments(
                            List.of(
                                UserFullProfileDTO.DetailedRoleAssignmentDTO.builder()
                                    .roleCode("DOCTOR")
                                    .build()))
                        .build()))
            .build());

    // Verify other hospital membership is still there
    assertTrue(user.getAssignments().stream().anyMatch(a -> otherHospId.equals(a.getHospitalId())));
    // Verify hospital 1 membership was replaced
    assertTrue(user.getAssignments().stream().anyMatch(a -> hospitalId.equals(a.getHospitalId())));
  }
}
