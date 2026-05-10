package com.res.hms.iam.application.dto;

import java.util.Set;
import java.util.UUID;

import com.res.hms.iam.domain.RoleCode;

public record HospitalMembershipResponse(
    UUID hospitalId,
    String hospitalName,
    String hospitalCode,
    Set<RoleCode> roles,
    String specialization,
    String department) {}
