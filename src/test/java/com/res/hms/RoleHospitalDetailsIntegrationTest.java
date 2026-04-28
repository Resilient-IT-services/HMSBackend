package com.res.hms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.user.dto.AdminHospitalDetailsDTO;
import com.res.hms.user.dto.NurseHospitalDetailsDTO;
import com.res.hms.user.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.user.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import com.res.hms.user.service.UserAggregateService;
import com.res.hms.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleHospitalDetailsIntegrationTest {

  @Autowired private UserService userService;
  @Autowired private UserAggregateService userAggregateService;
  @Autowired private TestDataHelper testDataHelper;
  @MockBean private HospitalContext hospitalContext;

  private UUID hospitalId;

  @BeforeEach
  void setup() {
    hospitalId = testDataHelper.createHospitalIfMissing("H1", "Hospital One");
    when(hospitalContext.isSuperAdmin()).thenReturn(true);
  }

  @Test
  void createNurseWithHospitalDetails() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .username("nurse_test")
            .password("pass")
            .fullName("Nurse Joy")
            .gender(com.res.hms.domain.Gender.FEMALE)
            .dob(java.time.LocalDate.of(1990, 1, 1))
            .nurseInfo(UserDetailsDTO.NurseInfo.builder().nursingLicenseNo("NL-123").build())
            .memberships(
                List.of(
                    UserDetailsDTO.HospitalMembershipDTO.builder()
                        .hospitalId(hospitalId)
                        .roleAssignments(
                            List.of(
                                UserDetailsDTO.RoleAssignmentDTO.builder()
                                    .roleCode("NURSE")
                                    .departmentName("Pediatrics")
                                    .nurseDetails(
                                        NurseHospitalDetailsDTO.builder()
                                            .floorLocation("2nd Floor")
                                            .shiftType(com.res.hms.domain.ShiftType.NIGHT)
                                            .build())
                                    .build()))
                        .build()))
            .build();

    UserDetailsDTO saved = userAggregateService.saveUserAggregate(request);
    assertNotNull(saved.getId());

    UserDetailsDTO fetched = userService.getFullProfile(saved.getId());

    // Check Nurse Global Profile
    assertNotNull(fetched.getNurseInfo());

    // Check Nurse Hospital Specific Details
    UserDetailsDTO.RoleAssignmentDTO assignment =
        fetched.getMemberships().stream()
            .filter(m -> hospitalId.equals(m.getHospitalId()))
            .flatMap(m -> m.getRoleAssignments().stream())
            .filter(ra -> "NURSE".equals(ra.getRoleCode()))
            .findFirst()
            .orElseThrow();

    assertEquals("Pediatrics", assignment.getDepartmentName());

    assertNotNull(assignment.getNurseDetails());
    assertEquals("2nd Floor", assignment.getNurseDetails().getFloorLocation());
    assertEquals(com.res.hms.domain.ShiftType.NIGHT, assignment.getNurseDetails().getShiftType());
  }

  @Test
  void createReceptionistWithHospitalDetails() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .username("recep_test")
            .password("pass")
            .fullName("Recep One")
            .gender(com.res.hms.domain.Gender.MALE)
            .dob(java.time.LocalDate.of(1995, 5, 5))
            .memberships(
                List.of(
                    UserDetailsDTO.HospitalMembershipDTO.builder()
                        .hospitalId(hospitalId)
                        .roleAssignments(
                            List.of(
                                UserDetailsDTO.RoleAssignmentDTO.builder()
                                    .roleCode("RECEPTIONIST")
                                    .receptionistDetails(
                                        ReceptionistHospitalDetailsDTO.builder()
                                            .deskLocation("Main Lobby A")
                                            .emergencyContactNumber("1234567890")
                                            .build())
                                    .build()))
                        .build()))
            .build();

    UserDetailsDTO saved = userAggregateService.saveUserAggregate(request);
    UserDetailsDTO fetched = userService.getFullProfile(saved.getId());

    UserDetailsDTO.RoleAssignmentDTO assignment =
        fetched.getMemberships().stream()
            .filter(m -> hospitalId.equals(m.getHospitalId()))
            .flatMap(m -> m.getRoleAssignments().stream())
            .filter(ra -> "RECEPTIONIST".equals(ra.getRoleCode()))
            .findFirst()
            .orElseThrow();

    assertNotNull(assignment.getReceptionistDetails());
    assertEquals("Main Lobby A", assignment.getReceptionistDetails().getDeskLocation());
    assertEquals("1234567890", assignment.getReceptionistDetails().getEmergencyContactNumber());
  }

  @Test
  void createAdminWithHospitalDetails() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .username("admin_test")
            .password("pass")
            .fullName("Admin One")
            .gender(com.res.hms.domain.Gender.MALE)
            .dob(java.time.LocalDate.of(1985, 10, 10))
            .memberships(
                List.of(
                    UserDetailsDTO.HospitalMembershipDTO.builder()
                        .hospitalId(hospitalId)
                        .roleAssignments(
                            List.of(
                                UserDetailsDTO.RoleAssignmentDTO.builder()
                                    .roleCode("ADMIN")
                                    .adminDetails(
                                        AdminHospitalDetailsDTO.builder()
                                            .officeLocation("Room 101")
                                            .departmentHead(true)
                                            .build())
                                    .build()))
                        .build()))
            .build();

    UserDetailsDTO saved = userAggregateService.saveUserAggregate(request);
    UserDetailsDTO fetched = userService.getFullProfile(saved.getId());

    UserDetailsDTO.RoleAssignmentDTO assignment =
        fetched.getMemberships().stream()
            .filter(m -> hospitalId.equals(m.getHospitalId()))
            .flatMap(m -> m.getRoleAssignments().stream())
            .filter(ra -> "ADMIN".equals(ra.getRoleCode()))
            .findFirst()
            .orElseThrow();

    assertNotNull(assignment.getAdminDetails());
    assertEquals("Room 101", assignment.getAdminDetails().getOfficeLocation());
    assertTrue(assignment.getAdminDetails().isDepartmentHead());
  }

  @Test
  void createPharmacistWithHospitalDetails() {
    UserDetailsDTO request =
        UserDetailsDTO.builder()
            .username("pharmacist_test")
            .password("pass")
            .fullName("Pharmacist One")
            .gender(com.res.hms.domain.Gender.MALE)
            .dob(java.time.LocalDate.of(1990, 1, 1))
            .pharmacistInfo(
                UserDetailsDTO.PharmacistInfo.builder()
                    .pciRegNo("PCI-123")
                    .qualification("B.Pharm")
                    .build())
            .memberships(
                List.of(
                    UserDetailsDTO.HospitalMembershipDTO.builder()
                        .hospitalId(hospitalId)
                        .roleAssignments(
                            List.of(
                                UserDetailsDTO.RoleAssignmentDTO.builder()
                                    .roleCode("PHARMACIST")
                                    .pharmacistDetails(
                                        PharmacistHospitalDetailsDTO.builder()
                                            .counterLocation("Counter 1")
                                            .pharmacyWing("West Wing")
                                            .pharmacyLicenseNo("PHARM-LIC-456")
                                            .build())
                                    .build()))
                        .build()))
            .build();

    UserDetailsDTO saved = userAggregateService.saveUserAggregate(request);
    UserDetailsDTO fetched = userService.getFullProfile(saved.getId());

    UserDetailsDTO.RoleAssignmentDTO assignment =
        fetched.getMemberships().stream()
            .filter(m -> hospitalId.equals(m.getHospitalId()))
            .flatMap(m -> m.getRoleAssignments().stream())
            .filter(ra -> "PHARMACIST".equals(ra.getRoleCode()))
            .findFirst()
            .orElseThrow();

    assertNotNull(assignment.getPharmacistDetails());
    assertEquals("Counter 1", assignment.getPharmacistDetails().getCounterLocation());
    assertEquals("West Wing", assignment.getPharmacistDetails().getPharmacyWing());
    assertEquals("PHARM-LIC-456", assignment.getPharmacistDetails().getPharmacyLicenseNo());
  }
}
