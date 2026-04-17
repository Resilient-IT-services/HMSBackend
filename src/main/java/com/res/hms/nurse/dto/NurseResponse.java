package com.res.hms.nurse.dto;

import com.res.hms.domain.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NurseResponse(
        Long id,
        String fullName,
        Gender gender,
        LocalDate dob,
        String phone,
        String department,
        LocalDateTime createdAt
) {
}
