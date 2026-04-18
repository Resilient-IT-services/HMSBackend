package com.res.hms.common.tenant;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.RoleCode;
import com.res.hms.user.security.HospitalUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HospitalContextTest {

    private final HospitalContext hospitalContext = new HospitalContext();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireHospitalIdReturnsHospitalIdForHospitalScopedUser() {
        HospitalUserPrincipal principal = new HospitalUserPrincipal(7L, 11L, "admin", "secret", Set.of(RoleCode.ADMIN), true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())
        );

        assertEquals(11L, hospitalContext.requireHospitalId());
    }

    @Test
    void requireHospitalIdRejectsMissingAuthentication() {
        ApiException exception = assertThrows(ApiException.class, hospitalContext::requireHospitalId);

        assertEquals("AUTH_ERROR", exception.getCode());
    }

    @Test
    void requireHospitalIdRejectsSuperAdmin() {
        HospitalUserPrincipal principal = new HospitalUserPrincipal(7L, null, "super", "secret", Set.of(RoleCode.SUPER_ADMIN), true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())
        );

        ApiException exception = assertThrows(ApiException.class, hospitalContext::requireHospitalId);

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
    }

    @Test
    void currentUserIdOrNullReturnsUserIdForPrincipal() {
        HospitalUserPrincipal principal = new HospitalUserPrincipal(9L, 11L, "user", "secret", Set.of(RoleCode.DOCTOR), true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())
        );

        assertEquals(9L, hospitalContext.currentUserIdOrNull());
    }

    @Test
    void currentUserIdOrNullReturnsNullForUnknownPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("plain-user", null, java.util.List.of())
        );

        assertNull(hospitalContext.currentUserIdOrNull());
    }
}
