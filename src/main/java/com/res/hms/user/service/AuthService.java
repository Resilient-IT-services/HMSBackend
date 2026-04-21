package com.res.hms.user.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.RoleCode;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.dto.UserProfileResponse;
import com.res.hms.user.security.HospitalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  public UserProfileResponse selectHospital(
      Long hospitalId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    HospitalUserPrincipal principal = getAuthenticatedPrincipal();
    Set<RoleCode> selectedRoles = collectRoles(principal, hospitalId);

    HospitalUserPrincipal refinedPrincipal =
        new HospitalUserPrincipal(
            principal.userId(),
            principal.username(),
            principal.password(),
            principal.active(),
            principal.hospitalRoles(),
            hospitalId,
            selectedRoles);

    updateSecurityContext(refinedPrincipal, httpRequest, httpResponse);
    return toProfileResponse(refinedPrincipal);
  }

  private HospitalUserPrincipal getAuthenticatedPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof HospitalUserPrincipal principal)) {
      throw new ApiException("AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED);
    }
    return principal;
  }

  private Set<RoleCode> collectRoles(HospitalUserPrincipal principal, Long hospitalId) {
    Set<RoleCode> roles = new HashSet<>(principal.hospitalRoles().getOrDefault(null, Set.of()));
    if (hospitalId != null) {
      Set<RoleCode> hospitalScopedRoles = principal.hospitalRoles().get(hospitalId);
      if (hospitalScopedRoles == null) {
        throw new ApiException(
            "AUTH_ERROR", "User has no access to this hospital", HttpStatus.FORBIDDEN);
      }
      roles.addAll(hospitalScopedRoles);
    }
    return roles;
  }

  private void updateSecurityContext(
      HospitalUserPrincipal principal,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    Authentication refinedAuth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(refinedAuth);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);
  }

  public void logout(HttpServletRequest request, HttpServletResponse response) {
    new SecurityContextLogoutHandler()
        .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
  }

  public UserProfileResponse currentUser() {
    return toProfileResponse(getAuthenticatedPrincipal());
  }

  private UserProfileResponse toProfileResponse(HospitalUserPrincipal principal) {
    List<HospitalMembershipResponse> memberships = new ArrayList<>();
    if (principal.hospitalRoles() != null) {
      for (Map.Entry<Long, Set<RoleCode>> entry : principal.hospitalRoles().entrySet()) {
        Long hospitalId = entry.getKey();
        Set<RoleCode> roles = entry.getValue();
        String specialization = null;
        String department = null;

        if (hospitalId != null) {
          if (roles.contains(RoleCode.DOCTOR)) {
            specialization = doctorRepository.findByUserId(principal.userId())
                .map(Doctor::getSpecialization)
                .orElse(null);
          }
          if (roles.contains(RoleCode.NURSE)) {
            department = nurseRepository.findByUserId(principal.userId())
                .map(Nurse::getDepartment)
                .orElse(null);
          }

          Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
          memberships.add(
              new HospitalMembershipResponse(
                  hospital.getId(), hospital.getName(), hospital.getCode(), roles, specialization, department));
        } else {
          memberships.add(
              new HospitalMembershipResponse(null, "Global Access", "GLOBAL", roles, null, null));
        }
      }
    }

    return new UserProfileResponse(
        principal.userId(), principal.hospitalId(), principal.username(), memberships);
  }
}
