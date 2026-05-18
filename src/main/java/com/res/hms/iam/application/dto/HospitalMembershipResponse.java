package com.res.hms.iam.application.dto;

import com.res.hms.iam.domain.RoleCode;
import java.util.Set;
import java.util.UUID;
import java.util.List;

public record HospitalMembershipResponse(
    UUID hospitalId,
    String hospitalName,
    String hospitalCode,
    Set<RoleCode> roles,
    List<String> specializations,
    List<String> departments) {}
