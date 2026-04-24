package com.res.hms.hospital.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HospitalResponse(
    UUID id,
    String name,
    String code,
    String address,
    String status,
    HospitalSettings settings,
    LocalDateTime createdAt) {}
