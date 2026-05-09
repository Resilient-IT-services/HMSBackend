package com.res.hms.iam.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.application.dto.ChangePasswordRequest;
import com.res.hms.iam.application.dto.LoginRequest;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
  private final UserAccountRepository userAccountRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

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

  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    HospitalUserPrincipal principal = getAuthenticatedPrincipal();
    UserAccount user =
        userAccountRepository
            .findById(principal.userId())
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

    if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
      throw new ApiException(
          "AUTH_ERROR", "Current password does not match", HttpStatus.BAD_REQUEST);
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userAccountRepository.save(user);
  }

  public UserDetailsDTO currentUser() {
    HospitalUserPrincipal principal = getAuthenticatedPrincipal();
    return userAccountRepository
        .findById(principal.userId())
        .map(userMapper::toDetailsDTO)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));
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
