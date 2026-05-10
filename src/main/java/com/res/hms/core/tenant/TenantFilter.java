package com.res.hms.core.tenant;

import com.res.hms.iam.application.security.HospitalUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class TenantFilter extends OncePerRequestFilter {

  private static final String HOSPITAL_ID_HEADER = "X-Hospital-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String hospitalIdHeader = request.getHeader(HOSPITAL_ID_HEADER);

    try {
      if (hospitalIdHeader != null && !hospitalIdHeader.isBlank()) {
        UUID hospitalId = UUID.fromString(hospitalIdHeader);

        if (hasAccessToHospital(hospitalId)) {
          TenantContext.setHospitalId(hospitalId);
          log.debug("Set Hospital ID from header: {}", hospitalId);
        } else {
          log.warn("User does not have access to hospital: {}", hospitalId);
        }
      }
      filterChain.doFilter(request, response);
    } catch (IllegalArgumentException e) {
      log.error("Invalid UUID format for X-Hospital-Id: {}", hospitalIdHeader);
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private boolean hasAccessToHospital(UUID hospitalId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return false;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof HospitalUserPrincipal userPrincipal) {
      if (userPrincipal.isSuperAdmin()) {
        return true;
      }
      return userPrincipal.hospitalRoles().containsKey(hospitalId);
    }

    return false;
  }
}
