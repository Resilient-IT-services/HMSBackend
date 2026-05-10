package com.res.hms.iam.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
class UserMembershipServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private ResourceLookup resourceLookup;

    @InjectMocks
    private UserMembershipService userMembershipService;

    private UserAccount user;
    private Role doctorRole;
    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setAssignments(new HashSet<>());
        
        doctorRole = new Role();
        doctorRole.setCode(RoleCode.DOCTOR);
        
        hospitalId = UUID.randomUUID();
    }

    @Test
    void testUpdateUserMemberships_AsSuperAdmin() {
        UserDetailsDTO request = new UserDetailsDTO();
        UserDetailsDTO.HospitalMembershipDTO membership = new UserDetailsDTO.HospitalMembershipDTO();
        membership.setHospitalId(hospitalId);
        
        UserDetailsDTO.RoleAssignmentDTO roleAssignment = new UserDetailsDTO.RoleAssignmentDTO();
        roleAssignment.setRoleCode("DOCTOR");
        roleAssignment.setDepartmentName("Cardiology");
        membership.setRoleAssignments(List.of(roleAssignment));
        
        request.setMemberships(List.of(membership));

        when(resourceLookup.findRequired(eq(RoleCode.DOCTOR), any(), anyString())).thenReturn(doctorRole);

        userMembershipService.updateUserMemberships(user, request, true, null);

        assertEquals(1, user.getAssignments().size());
        assertEquals(hospitalId, user.getAssignments().iterator().next().getHospitalId());
        assertEquals(RoleCode.DOCTOR, user.getAssignments().iterator().next().getRole().getCode());
    }

    @Test
    void testUpdateUserMemberships_RemoveMembership() {
        // Given user has existing assignment
        UserRoleAssignment existing = new UserRoleAssignment();
        existing.setHospitalId(hospitalId);
        existing.setRole(doctorRole);
        user.getAssignments().add(existing);

        // Request with NO memberships
        UserDetailsDTO request = new UserDetailsDTO();
        request.setMemberships(new ArrayList<>());

        userMembershipService.updateUserMemberships(user, request, true, null);

        assertTrue(user.getAssignments().isEmpty());
    }

    @Test
    void testUpdateUserMemberships_HospitalAdmin() {
        UserDetailsDTO request = new UserDetailsDTO();
        UserDetailsDTO.HospitalMembershipDTO membership = new UserDetailsDTO.HospitalMembershipDTO();
        membership.setHospitalId(hospitalId);
        
        UserDetailsDTO.RoleAssignmentDTO roleAssignment = new UserDetailsDTO.RoleAssignmentDTO();
        roleAssignment.setRoleCode("DOCTOR");
        roleAssignment.setDepartmentName("Updated Dept");
        membership.setRoleAssignments(List.of(roleAssignment));
        request.setMemberships(List.of(membership));

        // User already has this role in this hospital
        UserRoleAssignment existing = new UserRoleAssignment();
        existing.setHospitalId(hospitalId);
        existing.setRole(doctorRole);
        existing.setDepartmentName("Old Dept");
        user.getAssignments().add(existing);

        userMembershipService.updateUserMemberships(user, request, false, hospitalId);

        assertEquals(1, user.getAssignments().size());
        assertEquals("Updated Dept", user.getAssignments().iterator().next().getDepartmentName());
    }
}
