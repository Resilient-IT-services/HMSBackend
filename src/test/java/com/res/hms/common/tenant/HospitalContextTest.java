package com.res.hms.common.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.RoleCode;
import com.res.hms.user.security.HospitalUserPrincipal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class HospitalContextTest {

  private final HospitalContext hospitalContext = new HospitalContext();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void requireHospitalIdReturnsHospitalIdForHospitalScopedUser() {
    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            7L,
            "admin",
            "secret",
            true,
            Map.of(11L, Set.of(RoleCode.ADMIN)),
            11L,
            Set.of(RoleCode.ADMIN));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));

    assertEquals(11L, hospitalContext.requireHospitalId());
  }

  @Test
  void requireHospitalIdRejectsMissingAuthentication() {
    ApiException exception = assertThrows(ApiException.class, hospitalContext::requireHospitalId);

    assertEquals("AUTH_ERROR", exception.getCode());
  }

  @Test
  void requireHospitalIdRejectsSuperAdmin() {
    Map<Long, Set<RoleCode>> roles = new HashMap<>();
    roles.put(null, Set.of(RoleCode.SUPER_ADMIN));

    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            7L, "super", "secret", true, roles, null, Set.of(RoleCode.SUPER_ADMIN));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));

    ApiException exception = assertThrows(ApiException.class, hospitalContext::requireHospitalId);

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
  }

  @Test
  void currentUserIdOrNullReturnsUserIdForPrincipal() {
    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            9L,
            "user",
            "secret",
            true,
            Map.of(11L, Set.of(RoleCode.DOCTOR)),
            11L,
            Set.of(RoleCode.DOCTOR));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));

    assertEquals(9L, hospitalContext.currentUserIdOrNull());
  }

  @Test
  void currentUserIdOrNullReturnsNullForUnknownPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "plain-user", null, java.util.List.of()));

    assertNull(hospitalContext.currentUserIdOrNull());
  }
}
