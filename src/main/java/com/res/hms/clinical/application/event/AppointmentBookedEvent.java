package com.res.hms.clinical.application.event;

import java.util.UUID;

public record AppointmentBookedEvent(
    UUID appointmentId,
    UUID patientId,
    UUID hospitalId
) {}
