package com.res.hms.hospital.dto;

import com.res.hms.domain.HospitalStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record HospitalResponse(
    UUID id,
    String name,
    String code,
    String address,
    HospitalStatus status,
    HospitalSettings settings,
    LocalDateTime createdAt) {}
