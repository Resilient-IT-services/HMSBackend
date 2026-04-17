package com.res.hms.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        @NotNull(message = "patient_id is required")
        Long patientId,
        @NotNull(message = "doctor_id is required")
        Long doctorId,
        @NotNull(message = "appointment_time is required")
        @Future(message = "appointment_time must be in the future")
        LocalDateTime appointmentTime,
        String notes
) {
}
