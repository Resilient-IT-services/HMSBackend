package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public interface UserRoleValidatable {

  Set<RoleCode> roles();

  String specialization();

  String department();

  UUID hospitalId();

  Integer followUpDays();

  BigDecimal consultationFee();
}
