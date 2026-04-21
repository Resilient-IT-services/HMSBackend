package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import java.util.List;
import java.util.Set;

public record UserProfileResponse(
    Long userId,
    Long hospitalId,
    String username,
    List<HospitalMembershipResponse> hospitals) {}
