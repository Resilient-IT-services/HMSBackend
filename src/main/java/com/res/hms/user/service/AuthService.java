package com.res.hms.user.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.security.HospitalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;
  private final HospitalRepository hospitalRepository;
  private final DoctorRepository doctorRepository;
  private final NurseRepository nurseRepository;
  private final UserAccountRepository userAccountRepository;
  private final UserMapper userMapper;
  private final com.res.hms.common.tenant.HospitalContext hospitalContext;

  public void login(
      LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  request.username(), request.password()));

      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      securityContextRepository.saveContext(context, httpRequest, httpResponse);
    } catch (BadCredentialsException exception) {
      throw new ApiException(
          "AUTH_ERROR",
          "Invalid username or password",
          HttpStatus.UNAUTHORIZED,
          Map.of(),
          exception);
    }
  }

  public UserFullProfileDTO currentUser() {
    HospitalUserPrincipal principal = getAuthenticatedPrincipal();
    return userAccountRepository
        .findById(principal.userId())
        .map(userMapper::toFullProfileDTO)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));
  }

  public HospitalUserPrincipal getAuthenticatedPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof HospitalUserPrincipal principal)) {
      throw new ApiException("AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED);
    }
    return principal;
  }

  public void logout(HttpServletRequest request, HttpServletResponse response) {
    new SecurityContextLogoutHandler()
        .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
  }

}
