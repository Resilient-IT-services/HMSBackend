package com.res.hms.user.security;

import com.res.hms.domain.RoleCode;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record HospitalUserPrincipal(
    Long userId,
    String username,
    String password,
    boolean active,
    // Map of Hospital ID to Set of Roles. Hospital ID can be NULL for global roles.
    Map<Long, Set<RoleCode>> hospitalRoles,
    // The active context for this session
    Long hospitalId,
    Set<RoleCode> roleCodes)
    implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (roleCodes == null) {
      return Set.of();
    }
    return roleCodes.stream()
        .map(code -> new SimpleGrantedAuthority("ROLE_" + code.name()))
        .collect(Collectors.toSet());
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }

  public boolean isSuperAdmin() {
    return roleCodes != null && roleCodes.contains(RoleCode.SUPER_ADMIN);
  }
}
