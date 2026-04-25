package com.res.hms.hospital.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateHospitalRequest(
    @NotBlank(message = "name is required") String name,
    @NotBlank(message = "code is required") String code,
    String address,
    String gstin) {}
