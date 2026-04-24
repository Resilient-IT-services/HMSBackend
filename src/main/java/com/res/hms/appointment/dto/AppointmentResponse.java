package com.res.hms.appointment.dto;

import com.res.hms.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID patientId,
    String patientName,
    UUID doctorId,
    String doctorName,
    LocalDateTime appointmentTime,
    AppointmentStatus status,
    String notes,
    LocalDateTime createdAt) {}
