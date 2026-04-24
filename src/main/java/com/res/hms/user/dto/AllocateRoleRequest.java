package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record AllocateRoleRequest(
    @NotNull(message = "userId is required") UUID userId,
    UUID hospitalId, // Can be null for global roles
    Boolean isSuperuser,
    Set<RoleCode> roles, // If empty, removes access for this context
    String specialization,
    String department,
    BigDecimal consultationFee,
    Integer followUpDays)
    implements UserRoleValidatable {
  public AllocateRoleRequest {
    if (roles == null) {
      roles = new HashSet<>();
    }
  }
}
