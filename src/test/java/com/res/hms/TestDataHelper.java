package com.res.hms;

import com.res.hms.domain.Hospital;
import com.res.hms.domain.HospitalStatus;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.hospital.repository.HospitalRepository;
import com.res.hms.user.repository.RoleRepository;
import com.res.hms.user.repository.UserAccountRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDataHelper {

  @Autowired private HospitalRepository hospitalRepository;

  @Autowired private com.res.hms.patient.repository.PatientRepository patientRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public com.res.hms.domain.Patient createTestPatient(UUID hospitalId) {
    com.res.hms.domain.Patient p = new com.res.hms.domain.Patient();
    p.setHospitalId(hospitalId);
    p.setFullName("Test Patient");
    p.setGender(com.res.hms.domain.Gender.FEMALE);
    p.setPatientCode("P-" + UUID.randomUUID().toString().substring(0, 8));
    return patientRepository.save(p);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID createHospitalIfMissing(String code, String name) {
    return hospitalRepository
        .findByCode(code)
        .map(Hospital::getId)
        .orElseGet(
            () -> {
              Hospital h = new Hospital();
              h.setCode(code);
              h.setName(name);
              h.setAddress("Address");
              h.setStatus(HospitalStatus.ACTIVE);
              return hospitalRepository.save(h).getId();
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createUserIfMissing(
      String username,
      RoleCode roleCode,
      UUID hospitalId,
      String password,
      PasswordEncoder encoder,
      UserAccountRepository userRepo,
      RoleRepository roleRepo) {
    if (userRepo.existsByUsername(username)) {
      return;
    }

    UserAccount user = new UserAccount();
    user.setUsername(username);
    user.setPasswordHash(encoder.encode(password));
    user.setFullName(username + " Full Name");
    user.setActive(true);

    Role role =
        roleRepo
            .findByCode(roleCode)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(role);
    user.getAssignments().add(assignment);

    userRepo.save(user);
  }
}
