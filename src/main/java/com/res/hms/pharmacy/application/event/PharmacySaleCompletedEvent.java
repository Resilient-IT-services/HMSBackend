package com.res.hms.pharmacy.application.event;

import java.util.UUID;

public record PharmacySaleCompletedEvent(UUID saleId, UUID patientId, UUID hospitalId) {}
