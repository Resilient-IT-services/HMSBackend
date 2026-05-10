package com.res.hms.clinical.application.appointment.dto;

import com.res.hms.clinical.domain.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(
    @NotNull(message = "status is required") AppointmentStatus status) {}
