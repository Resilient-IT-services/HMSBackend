package com.res.hms.core.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import com.res.hms.iam.domain.RoleCode;

class HospitalContextTest {

  private final HospitalContext hospitalContext = new HospitalContext();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContext.clear();
  }

  @Test
  void requireHospitalIdReturnsHospitalIdForHospitalScopedUser() {
    UUID hospitalId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            userId,
            "admin",
            "admin@test.com",
            "secret",
            true,
            false,
            Map.of(hospitalId, Set.of(RoleCode.ADMIN)),
            hospitalId,
            Set.of(RoleCode.ADMIN));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));

    assertEquals(hospitalId, hospitalContext.requireHospitalId());
  }

  @Test
  void requireHospitalIdReturnsTenantIdIfPresent() {
    UUID hospitalId = UUID.randomUUID();
    TenantContext.setHospitalId(hospitalId);

    assertEquals(hospitalId, hospitalContext.requireHospitalId());
  }

  @Test
  void requireHospitalIdRejectsMissingAuthentication() {
    ApiException exception = assertThrows(ApiException.class, hospitalContext::requireHospitalId);

    assertEquals("AUTH_ERROR", exception.getCode());
  }

  @Test
  void requireHospitalIdRejectsSuperAdminWithoutSelection() {
    UUID userId = UUID.randomUUID();
    Map<UUID, Set<RoleCode>> roles = new HashMap<>();
    // No specific roles assigned, but is superuser
    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            userId, "super", "super@test.com", "secret", true, true, roles, null, Set.of());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));

    ApiException exception = assertThrows(ApiException.class, hospitalContext::requireHospitalId);

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
  }

  @Test
  void currentUserIdOrNullReturnsUserIdForPrincipal() {
    UUID hospitalId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            userId,
            "user",
            "user@test.com",
            "secret",
            true,
            false,
            Map.of(hospitalId, Set.of(RoleCode.DOCTOR)),
            hospitalId,
            Set.of(RoleCode.DOCTOR));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));

    assertEquals(userId, hospitalContext.currentUserIdOrNull());
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
