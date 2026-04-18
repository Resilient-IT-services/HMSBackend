package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public record AllocateRoleRequest(
        @NotNull(message = "userId is required")
        Long userId,
        
        Long hospitalId, // Can be null for global roles
        
        Set<RoleCode> roles // If empty, removes access for this context
) {
    public AllocateRoleRequest {
        if (roles == null) {
            roles = new HashSet<>();
        }
    }
}
