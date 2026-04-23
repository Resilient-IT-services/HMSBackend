package com.res.hms.doctor.dto;

import com.res.hms.domain.Gender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DoctorResponse(
    Long id,
    String fullName,
    Gender gender,
    LocalDate dob,
    String phone,
    String specialization,
    BigDecimal consultationFee,
    Integer followUpDays,
    LocalDateTime createdAt) {}
