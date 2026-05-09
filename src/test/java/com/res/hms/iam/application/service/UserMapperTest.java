package com.res.hms.iam.application.service;

import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.application.service.UserMapper;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

  @Mock private HospitalRepository hospitalRepository;

  private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

  @BeforeEach
  void setUp() {
    userMapper.hospitalRepository = hospitalRepository;
  }

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
