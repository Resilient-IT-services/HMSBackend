package com.res.hms.user.dto;

import com.res.hms.domain.Gender;
import com.res.hms.domain.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@ValidUserRole
public record CreateUserRequest(
    @NotBlank(message = "username is required") String username,
    @NotBlank(message = "password is required")
        @Size(min = 6, message = "password must be at least 6 characters")
        String password,
    @NotEmpty(message = "at least one role is required") Set<RoleCode> roles,
    @NotBlank(message = "fullName is required") String fullName,
    @NotNull(message = "gender is required") Gender gender,
    @NotNull(message = "dob is required") LocalDate dob,
    UUID hospitalId,
    String phone,
    String specialization,
    String department,
    BigDecimal consultationFee,
    Integer followUpDays)
    implements UserRoleValidatable {}
