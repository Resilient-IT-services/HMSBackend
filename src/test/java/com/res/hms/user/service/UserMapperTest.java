package com.res.hms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.*;
import com.res.hms.user.dto.UserSummaryDTO;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

  @Mock private HospitalRepository hospitalRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private NurseRepository nurseRepository;
  @Mock private ReceptionistProfileRepository receptionistProfileRepository;
  @Mock private DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  @Mock private AdminHospitalDetailsRepository adminHospitalDetailsRepository;
  @Mock private NurseHospitalDetailsRepository nurseHospitalDetailsRepository;
  @Mock private PharmacistHospitalDetailsRepository pharmacistHospitalDetailsRepository;
  @Mock private ReceptionistHospitalDetailsRepository receptionistHospitalDetailsRepository;
  @Mock private PharmacistProfileRepository pharmacistProfileRepository;
  @Mock private LabTechProfileRepository labTechProfileRepository;

  @InjectMocks private UserMapper userMapper;

  private final UUID hospitalId = UUID.nameUUIDFromBytes("hospital".getBytes());
  private final UUID doctorUserId = UUID.nameUUIDFromBytes("doctorUser".getBytes());

  @Test
  void toSummaryDTOMapsCorrectly() {
    UserAccount user = new UserAccount();
    user.setId(doctorUserId);
    user.setUsername("doctor1");

    Role doctorRole = new Role();
    doctorRole.setCode(RoleCode.DOCTOR);

    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setId(UUID.randomUUID());
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(doctorRole);
    user.getAssignments().add(assignment);

    var response = userMapper.toSummaryDTO(user);

    assertEquals(1, response.getMemberships().size());
    assertEquals(hospitalId, response.getMemberships().get(0).getHospitalId());
    assertEquals("DOCTOR", response.getMemberships().get(0).getRoles().get(0));
  }
}
