package com.res.hms.user.dto;

import jakarta.validation.constraints.NotNull;

public record SelectHospitalRequest(@NotNull(message = "hospitalId is required") Long hospitalId) {}
