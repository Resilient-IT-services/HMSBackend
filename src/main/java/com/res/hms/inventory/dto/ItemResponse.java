package com.res.hms.inventory.dto;

import java.time.LocalDateTime;

public record ItemResponse(
    Long id, String name, String category, String unit, LocalDateTime createdAt) {}
