package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import java.util.Set;

public record HospitalMembershipResponse(
        Long hospitalId,
        String hospitalName,
        String hospitalCode,
        Set<RoleCode> roles
) {
}
