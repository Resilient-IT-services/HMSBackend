package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id, String username, Set<RoleCode> roles, boolean active, LocalDateTime createdAt) {}
