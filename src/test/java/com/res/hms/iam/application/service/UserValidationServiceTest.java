package com.res.hms.iam.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.application.service.UserValidationService;
import com.res.hms.iam.infrastructure.repository.NurseProfileRepository;
import com.res.hms.iam.infrastructure.repository.LabTechProfileRepository;
import com.res.hms.iam.infrastructure.repository.PharmacistProfileRepository;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserValidationServiceTest {

  @Mock private DoctorProfileRepository doctorProfileRepository;
  @Mock private NurseProfileRepository nurseProfileRepository;
  @Mock private PharmacistProfileRepository pharmacistProfileRepository;
  @Mock private LabTechProfileRepository labTechProfileRepository;

  @InjectMocks private UserValidationService userValidationService;

  @Test
  void validateProfileUniquenessSkipsBlankDoctorLicenseNumberWhenNoDoctorRole() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .doctorInfo(UserDetailsDTO.DoctorInfo.builder().licenseNumber("  ").build())
            .build();

    userValidationService.validateProfileUniqueness(request, UUID.randomUUID());

    verifyNoInteractions(doctorProfileRepository);
  }

  @Test
  void validateProfileUniquenessRejectsDoctorCreateWithoutLicenseNumber() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .doctorInfo(UserDetailsDTO.DoctorInfo.builder().specialization("General").build())
            .memberships(doctorMemberships())
            .build();

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> userValidationService.validateProfileUniqueness(request, null));

    assertEquals("Doctor license number is required", ex.getMessage());
    verifyNoInteractions(doctorProfileRepository);
  }

  @Test
  void validateProfileUniquenessTrimsDoctorLicenseNumberBeforeChecking() {
    UUID userId = UUID.randomUUID();
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .doctorInfo(UserDetailsDTO.DoctorInfo.builder().licenseNumber("  MED-123  ").build())
            .memberships(doctorMemberships())
            .build();

    userValidationService.validateProfileUniqueness(request, userId);

    verify(doctorProfileRepository).existsByLicenseNumberAndUserIdNot("MED-123", userId);
  }

  @Test
  void validateProfileUniquenessRejectsDuplicateDoctorLicenseNumber() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .doctorInfo(UserDetailsDTO.DoctorInfo.builder().licenseNumber("MED-123").build())
            .memberships(doctorMemberships())
            .build();
    when(doctorProfileRepository.existsByLicenseNumber("MED-123")).thenReturn(true);

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> userValidationService.validateProfileUniqueness(request, null));

    assertEquals("Doctor license number already exists", ex.getMessage());
  }

  private List<UserDetailsDTO.HospitalMembershipDTO> doctorMemberships() {
    return List.of(
        UserDetailsDTO.HospitalMembershipDTO.builder()
            .roleAssignments(
                List.of(UserDetailsDTO.RoleAssignmentDTO.builder().roleCode("DOCTOR").build()))
            .build());
  }
}
