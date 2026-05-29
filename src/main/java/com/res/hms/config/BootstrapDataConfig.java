package com.res.hms.config;

import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
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
  public CommandLineRunner bootstrapData(
      HospitalRepository hospitalRepository,
      RoleRepository roleRepository,
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder) {
    return args -> {
      ensureRoles(roleRepository);
      ensureAdminUser(userAccountRepository, roleRepository, passwordEncoder);
    };
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
    String adminUsername = properties.adminUsername().toLowerCase();
    if (!userAccountRepository.existsByUsername(adminUsername)) {
      UserAccount admin = new UserAccount();
      admin.setUsername(adminUsername);
      admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
      admin.setActive(true);
      admin.setSuperuser(true);

      userAccountRepository.save(admin);
    }
  }
}
