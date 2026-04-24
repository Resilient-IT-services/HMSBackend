package com.res.hms.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemResponse(
    UUID id, String name, String category, String unit, LocalDateTime createdAt) {}
