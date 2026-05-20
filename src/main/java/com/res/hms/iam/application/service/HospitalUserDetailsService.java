package com.res.hms.iam.application.service;

import com.res.hms.hospital.domain.HospitalStatus;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;
  private final HospitalRepository hospitalRepository;

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseConcurrentHashMap"})
  public UserDetails loadUserByUsername(String username) {
    UserAccount userAccount =
        userAccountRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (!userAccount.isActive()) {
      throw new DisabledException("User account is inactive");
    }

    Map<UUID, Set<RoleCode>> hospitalRoles = new HashMap<>();

    for (UserContract assignment : userAccount.getContracts()) {
      if (!assignment.isActive() || assignment.getHospitalId() == null) {
        continue;
      }

      HospitalStatus hospitalStatus =
          assignment.getHospital() != null
              ? assignment.getHospital().getStatus()
              : hospitalRepository
                  .findById(assignment.getHospitalId())
                  .map(com.res.hms.hospital.domain.Hospital::getStatus)
                  .orElse(null);

      if (hospitalStatus != HospitalStatus.ACTIVE && hospitalStatus != HospitalStatus.ONBOARDING) {
        continue;
      }

      UUID hid = assignment.getHospitalId();
      RoleCode code = assignment.getRole().getCode();
      hospitalRoles.computeIfAbsent(hid, k -> new HashSet<>()).add(code);
    }

    Set<RoleCode> filteredRoleCodes =
        hospitalRoles.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

    return new HospitalUserPrincipal(
        userAccount.getId(),
        userAccount.getUsername(),
        userAccount.getEmail(),
        userAccount.getPasswordHash(),
        userAccount.isActive(),
        userAccount.isSuperuser(),
        hospitalRoles,
        null, // Active hospitalId
        filteredRoleCodes);
  }
}
