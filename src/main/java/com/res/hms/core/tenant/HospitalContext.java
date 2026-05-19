package com.res.hms.core.tenant;

import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class HospitalContext {

  public UUID requireHospitalId() {
    UUID tenantHospitalId = TenantContext.getHospitalId();
    if (tenantHospitalId != null) {
      return tenantHospitalId;
    }

    HospitalUserPrincipal principal =
        getPrincipal()
            .orElseThrow(
                () ->
                    new ApiException(
                        "AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED));

    if (principal.hospitalId() != null) {
      return principal.hospitalId();
    }

    if (principal.isSuperAdmin()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION",
          "A hospital must be selected even for Super Admin to perform operational tasks",
          HttpStatus.BAD_REQUEST);
    }

    throw new ApiException(
        "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
  }

  public UUID getHospitalIdOrNull() {
    UUID tenantHospitalId = TenantContext.getHospitalId();
    if (tenantHospitalId != null) {
      return tenantHospitalId;
    }
    return getPrincipal().map(HospitalUserPrincipal::hospitalId).orElse(null);
  }

  public boolean isSuperAdmin() {
    return getPrincipal().map(HospitalUserPrincipal::isSuperAdmin).orElse(false);
  }

  public UUID currentUserIdOrNull() {
    return getPrincipal().map(HospitalUserPrincipal::userId).orElse(null);
  }

  public boolean hasRole(com.res.hms.iam.domain.RoleCode roleCode) {
    return getPrincipal()
        .map(p -> p.roleCodes() != null && p.roleCodes().contains(roleCode))
        .orElse(false);
  }

  public boolean isReceptionist() {
    return hasRole(com.res.hms.iam.domain.RoleCode.RECEPTIONIST) && !isSuperAdmin() && !hasRole(com.res.hms.iam.domain.RoleCode.ADMIN);
  }

  private Optional<HospitalUserPrincipal> getPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof HospitalUserPrincipal hospitalUserPrincipal) {
      return Optional.of(hospitalUserPrincipal);
    }
    return Optional.empty();
  }
}
