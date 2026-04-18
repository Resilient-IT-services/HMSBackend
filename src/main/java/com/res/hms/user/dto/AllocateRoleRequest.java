package com.res.hms.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.res.hms.domain.RoleCode;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public record AllocateRoleRequest(
        @NotNull(message = "user_id is required")
        @JsonProperty("user_id")
        Long userId,
        
        @JsonProperty("hospital_id")
        Long hospitalId, // Can be null for global roles
        
        Set<RoleCode> roles // If empty, removes access for this context
) {
    public AllocateRoleRequest {
        if (roles == null) {
            roles = new HashSet<>();
        }
    }
}
