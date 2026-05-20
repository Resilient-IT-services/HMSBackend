package com.res.hms.core.tenant;

import com.res.hms.hospital.domain.HospitalStatus;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

  private static final String HOSPITAL_ID_HEADER = "X-Hospital-Id";
  private final UserAccountRepository userAccountRepository;
  private final HospitalRepository hospitalRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String hospitalIdHeader = request.getHeader(HOSPITAL_ID_HEADER);

    try {
      if (hospitalIdHeader != null && !hospitalIdHeader.isBlank()) {
        UUID hospitalId = UUID.fromString(hospitalIdHeader);

        if (hasAccessToHospital(hospitalId, response)) {
          TenantContext.setHospitalId(hospitalId);
          log.debug("Set Hospital ID from header: {}", hospitalId);
        } else {
          log.warn("User does not have access to hospital: {}", hospitalId);
          return; // Response already written in hasAccessToHospital
        }
      }
      filterChain.doFilter(request, response);
    } catch (IllegalArgumentException e) {
      log.error("Invalid UUID format for X-Hospital-Id: {}", hospitalIdHeader);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Hospital ID format");
    } finally {
      TenantContext.clear();
    }
  }

  private boolean hasAccessToHospital(UUID hospitalId, HttpServletResponse response)
      throws IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
      return false;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof HospitalUserPrincipal userPrincipal) {
      // Validate User is still active
      var userOpt = userAccountRepository.findById(userPrincipal.userId());
      if (userOpt.isEmpty() || !userOpt.get().isActive()) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "User account is inactive");
        return false;
      }

      var user = userOpt.get();

      var hospitalOpt = hospitalRepository.findById(hospitalId);
      if (hospitalOpt.isEmpty()
          || (hospitalOpt.get().getStatus() != HospitalStatus.ACTIVE
              && hospitalOpt.get().getStatus() != HospitalStatus.ONBOARDING)) {
        response.sendError(
            HttpServletResponse.SC_FORBIDDEN, "Hospital is inactive or does not exist");
        return false;
      }

      // Super admin can access any active hospital
      if (userPrincipal.isSuperAdmin()) {
        return true;
      }

      // Validate Contract (Role Assignment) and Hospital for regular users
      boolean hasActiveContract =
          user.getContracts().stream()
              .anyMatch(a -> a.getHospitalId().equals(hospitalId) && a.isActive());

      if (!hasActiveContract) {
        response.sendError(
            HttpServletResponse.SC_FORBIDDEN, "No active contract found for this active hospital");
        return false;
      }

      return true;
    }

    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication principal");
    return false;
  }
}
