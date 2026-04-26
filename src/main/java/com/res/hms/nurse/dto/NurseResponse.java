package com.res.hms.nurse.dto;

import com.res.hms.domain.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record NurseResponse(
    UUID id, String fullName, Gender gender, LocalDate dob, String phone, String department) {}
