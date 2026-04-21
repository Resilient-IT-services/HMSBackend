package com.res.hms.user.dto;

import java.util.List;

public record UserResponse(
    Long id,
    String username,
    String fullName,
    com.res.hms.domain.Gender gender,
    java.time.LocalDate dob,
    String phone,
    List<HospitalMembershipResponse> memberships,
    boolean active,
    boolean deleted,
    java.time.LocalDateTime createdAt) {}
