package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        RoleCode role,
        boolean active,
        LocalDateTime createdAt
) {
}
