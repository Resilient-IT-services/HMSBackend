package com.res.hms.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
    @NotNull(message = "patientId is required") UUID patientId,
    @NotNull(message = "doctorId is required") UUID doctorId,
    @NotNull(message = "appointmentTime is required")
        @Future(message = "appointmentTime must be in the future")
        LocalDateTime appointmentTime,
    String notes) {}
