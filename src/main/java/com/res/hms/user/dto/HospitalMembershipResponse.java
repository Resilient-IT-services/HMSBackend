package com.res.hms.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.res.hms.domain.RoleCode;
import java.util.Set;

public record HospitalMembershipResponse(
        @JsonProperty("hospital_id")
        Long hospitalId,
        @JsonProperty("hospital_name")
        String hospitalName,
        @JsonProperty("hospital_code")
        String hospitalCode,
        Set<RoleCode> roles
) {
}
