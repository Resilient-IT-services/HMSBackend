package com.res.hms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.res.hms.repository.HospitalRepository;
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

  @InjectMocks private UserMapper userMapper;

  private final UUID hospitalId = UUID.nameUUIDFromBytes("hospital".getBytes());
  private final UUID doctorUserId = UUID.nameUUIDFromBytes("doctorUser".getBytes());
  private final UUID nurseUserId = UUID.nameUUIDFromBytes("nurseUser".getBytes());

  @Test
  void toResponseMapsSpecializationForDoctor() {
    UserAccount user = new UserAccount();
    user.setId(doctorUserId);
    user.setUsername("doctor1");

    Hospital hospital = new Hospital();
    hospital.setId(hospitalId);
    hospital.setName("Test Hospital");
    hospital.setCode("TEST");

    Role doctorRole = new Role();
    doctorRole.setCode(RoleCode.DOCTOR);

    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(doctorRole);
    user.getAssignments().add(assignment);

    Doctor doctor = new Doctor();
    doctor.setSpecialization("Cardiology");

    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(doctorRepository.findByUserId(doctorUserId)).thenReturn(Optional.of(doctor));

    var response = userMapper.toResponse(user);

    assertEquals(1, response.memberships().size());
    assertEquals("Cardiology", response.memberships().getFirst().specialization());
    assertNull(response.memberships().getFirst().department());
  }

  @Test
  void toResponseMapsDepartmentForNurse() {
    UserAccount user = new UserAccount();
    user.setId(nurseUserId);
    user.setUsername("nurse1");

    Hospital hospital = new Hospital();
    hospital.setId(hospitalId);
    hospital.setName("Test Hospital");
    hospital.setCode("TEST");

    Role nurseRole = new Role();
    nurseRole.setCode(RoleCode.NURSE);

    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(nurseRole);
    user.getAssignments().add(assignment);

    Nurse nurse = new Nurse();
    nurse.setDepartment("Emergency");

    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(nurseRepository.findByUserId(nurseUserId)).thenReturn(Optional.of(nurse));

    var response = userMapper.toResponse(user);

    assertEquals(1, response.memberships().size());
    assertEquals("Emergency", response.memberships().getFirst().department());
    assertNull(response.memberships().getFirst().specialization());
  }
}
