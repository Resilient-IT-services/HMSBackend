package com.res.hms.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    Long id,
    String username,
    List<HospitalMembershipResponse> memberships,
    boolean active,
    LocalDateTime createdAt) {}
