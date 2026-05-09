package com.res.hms;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.service.UserAggregateService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityExhaustiveIntegrationTest {

  @Autowired private UserAggregateService userAggregateService;
  @Autowired private TestDataHelper testDataHelper;

  private UUID hospitalA;
  private UUID hospitalB;

  @BeforeEach
  void setup() {
    hospitalA = testDataHelper.createHospitalIfMissing("H-A", "Hospital A");
    hospitalB = testDataHelper.createHospitalIfMissing("H-B", "Hospital B");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void hospitalAdminCreateUserWithoutRole_ShouldFail() {
    // Note: In our setup, HospitalContext needs to be populated.
    // Usually via a filter. In integration tests, we might need to mock it or set it.
    // For now, testing the business logic inside UserAggregateService

    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .username("new_user")
            .fullName("New User")
            .gender(com.res.hms.core.domain.Gender.MALE)
            .dob(java.time.LocalDate.of(1990, 1, 1))
            .memberships(List.of()) // No memberships
            .build();

    assertThrows(ApiException.class, () -> userAggregateService.saveUserAggregate(request));
  }

  @Test
  @WithMockUser(roles = "SUPER_ADMIN")
  void superAdminCanCreateUserInAnyHospital() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .username("sa_user")
            .fullName("SA User")
            .gender(com.res.hms.core.domain.Gender.MALE)
            .dob(java.time.LocalDate.of(1990, 1, 1))
            .doctorInfo(
                UserDetailsDTO.DoctorInfo.builder()
                    .specialization("General")
                    .licenseNumber("SEC-DOC-001")
                    .build())
            .memberships(
                List.of(
                    UserDetailsDTO.HospitalMembershipDTO.builder()
                        .hospitalId(hospitalA)
                        .roleAssignments(
                            List.of(
                                UserDetailsDTO.RoleAssignmentDTO.builder()
                                    .roleCode("DOCTOR")
                                    .build()))
                        .build()))
            .build();

    userAggregateService.saveUserAggregate(request);
  }
}
