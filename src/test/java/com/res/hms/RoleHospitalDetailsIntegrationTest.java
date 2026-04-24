package com.res.hms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.res.hms.domain.RoleCode;
import com.res.hms.user.dto.*;
import com.res.hms.user.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleHospitalDetailsIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private TestDataHelper testDataHelper;

    private UUID hospitalId;

    @BeforeEach
    void setup() {
        hospitalId = testDataHelper.createHospitalIfMissing("H1", "Hospital One");
    }

    @Test
    void createNurseWithHospitalDetails() {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("nurse_test")
                .password("pass")
                .fullName("Nurse Joy")
                .gender(com.res.hms.domain.Gender.FEMALE)
                .dob(java.time.LocalDate.of(1990, 1, 1))
                .memberships(List.of(new UserRequestDTO.MembershipDTO(hospitalId, Set.of(RoleCode.NURSE))))
                .department("Pediatrics")
                .floorLocation("2nd Floor")
                .shiftType("NIGHT")
                .build();

        UserFullProfileDTO saved = userService.createUser(request);
        assertNotNull(saved.getId());

        UserFullProfileDTO fetched = userService.getFullProfile(saved.getId());
        
        // Check Nurse Global Profile
        assertNotNull(fetched.getNurseInfo());
        assertEquals("Pediatrics", fetched.getNurseInfo().getDepartment());

        // Check Nurse Hospital Specific Details (Nested)
        UserSummaryDTO.RoleAssignmentDTO assignment = fetched.getMemberships().stream()
                .filter(m -> hospitalId.equals(m.getHospitalId()))
                .flatMap(m -> m.getRoleAssignments().stream())
                .filter(ra -> "NURSE".equals(ra.getRoleCode()))
                .findFirst().orElseThrow();
        
        assertNotNull(assignment.getHospitalDetails());
        assertTrue(assignment.getHospitalDetails() instanceof NurseHospitalDetailsDTO);
        NurseHospitalDetailsDTO details = (NurseHospitalDetailsDTO) assignment.getHospitalDetails();
        assertEquals("2nd Floor", details.getFloorLocation());
        assertEquals("NIGHT", details.getShiftType());
    }

    @Test
    void createReceptionistWithHospitalDetails() {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("recep_test")
                .password("pass")
                .fullName("Recep One")
                .gender(com.res.hms.domain.Gender.MALE)
                .dob(java.time.LocalDate.of(1995, 5, 5))
                .memberships(List.of(new UserRequestDTO.MembershipDTO(hospitalId, Set.of(RoleCode.RECEPTIONIST))))
                .deskLocation("Main Lobby A")
                .emergencyContactNumber("1234567890")
                .build();

        UserFullProfileDTO saved = userService.createUser(request);
        UserFullProfileDTO fetched = userService.getFullProfile(saved.getId());

        UserSummaryDTO.RoleAssignmentDTO assignment = fetched.getMemberships().stream()
                .filter(m -> hospitalId.equals(m.getHospitalId()))
                .flatMap(m -> m.getRoleAssignments().stream())
                .filter(ra -> "RECEPTIONIST".equals(ra.getRoleCode()))
                .findFirst().orElseThrow();

        assertNotNull(assignment.getHospitalDetails());
        ReceptionistHospitalDetailsDTO details = (ReceptionistHospitalDetailsDTO) assignment.getHospitalDetails();
        assertEquals("Main Lobby A", details.getDeskLocation());
        assertEquals("1234567890", details.getEmergencyContactNumber());
    }

    @Test
    void createAdminWithHospitalDetails() {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("admin_test")
                .password("pass")
                .fullName("Admin One")
                .gender(com.res.hms.domain.Gender.MALE)
                .dob(java.time.LocalDate.of(1985, 10, 10))
                .memberships(List.of(new UserRequestDTO.MembershipDTO(hospitalId, Set.of(RoleCode.ADMIN))))
                .officeLocation("Room 101")
                .departmentHead(true)
                .build();

        UserFullProfileDTO saved = userService.createUser(request);
        UserFullProfileDTO fetched = userService.getFullProfile(saved.getId());

        UserSummaryDTO.RoleAssignmentDTO assignment = fetched.getMemberships().stream()
                .filter(m -> hospitalId.equals(m.getHospitalId()))
                .flatMap(m -> m.getRoleAssignments().stream())
                .filter(ra -> "ADMIN".equals(ra.getRoleCode()))
                .findFirst().orElseThrow();

        assertNotNull(assignment.getHospitalDetails());
        AdminHospitalDetailsDTO details = (AdminHospitalDetailsDTO) assignment.getHospitalDetails();
        assertEquals("Room 101", details.getOfficeLocation());
        assertTrue(details.isDepartmentHead());
    }
}
