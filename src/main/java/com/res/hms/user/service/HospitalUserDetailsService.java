package com.res.hms.user.service;

import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.security.HospitalUserPrincipal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserAccount userAccount =
        userAccountRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (!userAccount.isActive()) {
      throw new DisabledException("User account is inactive");
    }

    Map<Long, Set<com.res.hms.domain.RoleCode>> hospitalRoles = new HashMap<>();
    Set<com.res.hms.domain.RoleCode> allRoles = new HashSet<>();

    for (UserRoleAssignment assignment : userAccount.getAssignments()) {
      Long hid = assignment.getHospitalId();
      com.res.hms.domain.RoleCode code = assignment.getRole().getCode();
      hospitalRoles.computeIfAbsent(hid, k -> new HashSet<>()).add(code);
      allRoles.add(code);
    }

    return new HospitalUserPrincipal(
        userAccount.getId(),
        userAccount.getUsername(),
        userAccount.getPasswordHash(),
        userAccount.isActive(),
        hospitalRoles,
        null, // Active hospitalId
        allRoles // We load ALL roles so @PreAuthorize works, but HospitalContext will enforce
        // hospitalId selection
        );
  }
}
