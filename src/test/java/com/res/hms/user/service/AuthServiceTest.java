package com.res.hms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.RoleCode;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.security.HospitalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private SecurityContextRepository securityContextRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private HttpServletRequest httpServletRequest;
  @Mock private HttpServletResponse httpServletResponse;

  @InjectMocks private AuthService authService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void loginAuthenticatesAndPersistsSecurityContext() {
    Map<Long, Set<RoleCode>> roles = new HashMap<>();
    roles.put(null, Set.of(RoleCode.DOCTOR));

    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            1L, "doctor", "secret", true, roles, null, Set.of(RoleCode.DOCTOR));
    Authentication authentication = new TestingAuthenticationToken(principal, null);
    when(authenticationManager.authenticate(any())).thenReturn(authentication);

    authService.login(
        new LoginRequest("doctor", "secret"), httpServletRequest, httpServletResponse);

    ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
    verify(securityContextRepository).saveContext(contextCaptor.capture(), any(), any());
    assertNotNull(contextCaptor.getValue().getAuthentication());
  }

  @Test
  void loginRejectsBadCredentials() {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                authService.login(
                    new LoginRequest("doctor", "bad"), httpServletRequest, httpServletResponse));

    assertEquals("AUTH_ERROR", exception.getCode());
  }

  @Test
  void currentUserReturnsPrincipalDetails() {
    Map<Long, Set<RoleCode>> roles = new HashMap<>();
    roles.put(null, Set.of(RoleCode.ADMIN));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(
                new HospitalUserPrincipal(
                    2L, "admin", "secret", true, roles, null, Set.of(RoleCode.ADMIN)),
                null));

    var response = authService.currentUser();

    assertEquals(2L, response.userId());
    assertTrue(
        response.hospitals().stream().anyMatch(h -> h.roles().contains(RoleCode.ADMIN)));
  }

  @Test
  void currentUserRejectsMissingAuthentication() {
    ApiException exception = assertThrows(ApiException.class, authService::currentUser);

    assertEquals("AUTH_ERROR", exception.getCode());
  }

  @Test
  void currentUserRejectsUnexpectedPrincipalType() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("anonymous", null));

    ApiException exception = assertThrows(ApiException.class, authService::currentUser);

    assertEquals("AUTH_ERROR", exception.getCode());
  }
}
