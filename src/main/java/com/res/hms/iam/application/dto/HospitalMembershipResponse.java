package com.res.hms.iam.application.dto;

import com.res.hms.iam.domain.RoleCode;
import java.util.Set;
import java.util.UUID;

public record HospitalMembershipResponse(
    UUID hospitalId,
    String hospitalName,
    String hospitalCode,
    Set<RoleCode> roles,
    String specialization,
    String department) {}
