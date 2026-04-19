package com.res.hms.config;

import com.res.hms.domain.Hospital;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BootstrapDataConfig {

  private final AppBootstrapProperties properties;

  @Bean
  CommandLineRunner bootstrapData(
      HospitalRepository hospitalRepository,
      RoleRepository roleRepository,
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder) {
    return args -> {
      Hospital hospital = ensureHospital(hospitalRepository);
      ensureRoles(roleRepository);
      ensureAdminUser(userAccountRepository, roleRepository, passwordEncoder);
    };
  }

  private Hospital ensureHospital(HospitalRepository hospitalRepository) {
    return hospitalRepository
        .findByCode(properties.hospitalCode())
        .orElseGet(
            () -> {
              Hospital newHospital = new Hospital();
              newHospital.setCode(properties.hospitalCode());
              newHospital.setName(properties.hospitalName());
              newHospital.setStatus("ACTIVE");
              return hospitalRepository.save(newHospital);
            });
  }

  private void ensureRoles(RoleRepository roleRepository) {
    Arrays.stream(RoleCode.values())
        .forEach(
            roleCode ->
                roleRepository
                    .findByCode(roleCode)
                    .orElseGet(
                        () -> {
                          Role role = new Role();
                          role.setCode(roleCode);
                          role.setName(roleCode.name());
                          role.setDescription(roleCode.name() + " role");
                          role.setSystem(true);
                          role.setActive(true);
                          return roleRepository.save(role);
                        }));
  }

  private void ensureAdminUser(
      UserAccountRepository userAccountRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    if (!userAccountRepository.existsByUsername(properties.adminUsername())) {
      UserAccount admin = new UserAccount();
      admin.setUsername(properties.adminUsername());
      admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
      admin.setActive(true);
      admin.setFullName("Super Admin");

      UserRoleAssignment assignment = new UserRoleAssignment();
      assignment.setUser(admin);
      assignment.setHospitalId(null);
      assignment.setRole(roleRepository.findByCode(RoleCode.SUPER_ADMIN).orElseThrow());
      admin.getAssignments().add(assignment);

      userAccountRepository.save(admin);
    }
  }
}
