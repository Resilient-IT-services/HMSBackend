package com.res.hms.user.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
    UUID userId, UUID hospitalId, String username, List<HospitalMembershipResponse> hospitals) {}
