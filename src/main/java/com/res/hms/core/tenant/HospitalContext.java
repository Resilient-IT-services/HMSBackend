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
