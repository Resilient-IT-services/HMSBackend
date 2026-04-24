package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import java.util.Set;
import java.util.UUID;

public record HospitalMembershipResponse(
    UUID hospitalId,
    String hospitalName,
    String hospitalCode,
    Set<RoleCode> roles,
    String specialization,
    String department) {}
