package com.res.hms.user.dto;

import java.util.List;

public record UserProfileResponse(
    Long userId,
    Long hospitalId,
    String username,
    List<HospitalMembershipResponse> hospitals) {}
