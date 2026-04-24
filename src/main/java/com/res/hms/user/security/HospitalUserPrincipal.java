package com.res.hms.user.security;

import com.res.hms.domain.RoleCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record HospitalUserPrincipal(
    UUID userId,
    String username,
    String email,
    String password,
    boolean active,
    boolean superuser,
    // Map of Hospital ID to Set of Roles. Hospital ID can be NULL for global roles.
    Map<UUID, Set<RoleCode>> hospitalRoles,
    // The active context for this session
    UUID hospitalId,
    Set<RoleCode> roleCodes)
    implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new HashSet<>();
    if (roleCodes != null) {
      authorities.addAll(
          roleCodes.stream()
              .map(code -> new SimpleGrantedAuthority("ROLE_" + code.name()))
              .collect(Collectors.toSet()));
    }
    if (superuser) {
      authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }
    return authorities;
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
    return superuser;
  }
}
