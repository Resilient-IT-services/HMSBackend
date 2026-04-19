package com.res.hms.appointment.dto;

import com.res.hms.domain.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(
    @NotNull(message = "status is required") AppointmentStatus status) {}
