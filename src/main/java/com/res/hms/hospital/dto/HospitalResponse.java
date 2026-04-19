package com.res.hms.hospital.dto;

import java.time.LocalDateTime;

public record HospitalResponse(
    Long id, String name, String code, String address, String status, LocalDateTime createdAt) {}
