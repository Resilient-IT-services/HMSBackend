package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;

public record UserProfileResponse(
        Long userId,
        Long hospitalId,
        String username,
        RoleCode role
) {
}
