package com.res.hms.pharmacy.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicineResponse(
    UUID id, String name, String composition, String manufacturer, LocalDateTime createdAt) {}
