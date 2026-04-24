package com.res.hms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.RoleCode;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.security.HospitalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
  @Mock private DoctorRepository doctorRepository;
  @Mock private NurseRepository nurseRepository;
  @Mock private com.res.hms.common.tenant.HospitalContext hospitalContext;
  @Mock private HttpServletRequest httpServletRequest;
  @Mock private HttpServletResponse httpServletResponse;

  @InjectMocks private AuthService authService;

  private final UUID hospitalId = UUID.nameUUIDFromBytes("hospital".getBytes());
  private final UUID userId = UUID.nameUUIDFromBytes("user".getBytes());

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void currentUserReturnsRoleSpecificDetails() {
    Map<UUID, Set<RoleCode>> roles = new HashMap<>();
    roles.put(hospitalId, Set.of(RoleCode.DOCTOR));

    Hospital hospital = new Hospital();
    hospital.setId(hospitalId);
    hospital.setName("Test Hospital");
    hospital.setCode("TEST");

    Doctor doctor = new Doctor();
    doctor.setSpecialization("Pediatrics");

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(
                new HospitalUserPrincipal(
                    userId,
                    "doctor1",
                    "doctor@test.com",
                    "secret",
                    true,
                    false,
                    roles,
                    hospitalId,
                    Set.of(RoleCode.DOCTOR)),
                null));

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(doctorRepository.findByUserId(userId)).thenReturn(Optional.of(doctor));

    var response = authService.currentUser();

    assertEquals(1, response.hospitals().size());
    assertEquals("Pediatrics", response.hospitals().getFirst().specialization());
  }

  @Test
  void loginAuthenticatesAndPersistsSecurityContext() {
    Map<UUID, Set<RoleCode>> roles = new HashMap<>();
    roles.put(null, Set.of(RoleCode.DOCTOR));

    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            userId, "doctor", "doctor@test.com", "secret", true, false, roles, null, Set.of(RoleCode.DOCTOR));
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
    Map<UUID, Set<RoleCode>> roles = new HashMap<>();
    roles.put(null, Set.of(RoleCode.ADMIN));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(
                new HospitalUserPrincipal(
                    userId, "admin", "admin@test.com", "secret", true, false, roles, null, Set.of(RoleCode.ADMIN)),
                null));

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(null);

    var response = authService.currentUser();

    assertEquals(userId, response.userId());
    assertTrue(response.hospitals().stream().anyMatch(h -> h.roles().contains(RoleCode.ADMIN)));
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
