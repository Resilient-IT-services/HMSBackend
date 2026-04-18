package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import java.util.Set;

public record LoginResponse(
        Long userId,
        Long hospitalId,
        String username,
        Set<RoleCode> roles
) {
}
