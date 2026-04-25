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
import com.res.hms.domain.UserAccount;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import com.res.hms.repository.UserAccountRepository;
import java.util.List;
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
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private UserMapper userMapper;

  @InjectMocks private AuthService authService;

  private final UUID hospitalId = UUID.nameUUIDFromBytes("hospital".getBytes());
  private final UUID userId = UUID.nameUUIDFromBytes("user".getBytes());

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void currentUserReturnsRoleSpecificDetails() {
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
                    new HashMap<>(),
                    hospitalId,
                    Set.of(RoleCode.DOCTOR)),
                null));

    UserAccount user = new UserAccount();
    user.setId(userId);
    UserFullProfileDTO mappedResponse = UserFullProfileDTO.builder()
        .id(userId)
        .hospitalId(hospitalId)
        .username("doctor1")
        .email("doctor@test.com")
        .superuser(false)
        .memberships(List.of(new UserSummaryDTO.HospitalMembershipDTO(hospitalId, List.of("DOCTOR"))))
        .build();

    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toFullProfileDTO(user)).thenReturn(mappedResponse);

    var response = authService.currentUser();

    assertEquals(1, response.getMemberships().size());
  }

  @Test
  void loginAuthenticatesAndPersistsSecurityContext() {
    Map<UUID, Set<RoleCode>> roles = new HashMap<>();
    roles.put(null, Set.of(RoleCode.DOCTOR));

    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            userId,
            "doctor",
            "doctor@test.com",
            "secret",
            true,
            false,
            roles,
            null,
            Set.of(RoleCode.DOCTOR));
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
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(
                new HospitalUserPrincipal(
                    userId,
                    "admin",
                    "admin@test.com",
                    "secret",
                    true,
                    false,
                    new HashMap<>(),
                    null,
                    Set.of(RoleCode.ADMIN)),
                null));

    UserAccount user = new UserAccount();
    user.setId(userId);
    UserFullProfileDTO mappedResponse = UserFullProfileDTO.builder()
        .id(userId)
        .username("admin")
        .email("admin@test.com")
        .superuser(false)
        .memberships(List.of(new UserSummaryDTO.HospitalMembershipDTO(null, List.of("ADMIN"))))
        .build();

    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toFullProfileDTO(user)).thenReturn(mappedResponse);

    var response = authService.currentUser();

    assertEquals(userId, response.getId());
    assertTrue(response.getMemberships().stream().anyMatch(h -> h.getRoles().contains("ADMIN")));
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
