package com.res.hms.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.res.hms.domain.RoleCode;
import java.util.List;
import java.util.Set;

public record UserProfileResponse(
        @JsonProperty("user_id")
        Long userId,
        @JsonProperty("hospital_id")
        Long hospitalId,
        String username,
        Set<RoleCode> roles,
        List<HospitalMembershipResponse> hospitals
) {
}
