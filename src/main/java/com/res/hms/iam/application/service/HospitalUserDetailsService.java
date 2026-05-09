package com.res.hms.iam.application.service;

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    for (UserRoleAssignment assignment : userAccount.getAssignments()) {
      UUID hid = assignment.getHospitalId();
      RoleCode code = assignment.getRole().getCode();
      hospitalRoles.computeIfAbsent(hid, k -> new HashSet<>()).add(code);
    }

    return new HospitalUserPrincipal(
        userAccount.getId(),
        userAccount.getUsername(),
        userAccount.getEmail(),
        userAccount.getPasswordHash(),
        userAccount.isActive(),
        userAccount.isSuperuser(),
        hospitalRoles,
        null, // Active hospitalId
        userAccount.getRoleCodes() // Use helper method to include all roles
        );
  }
}
