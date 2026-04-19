package com.res.hms.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateItemRequest(
    @NotBlank(message = "name is required") String name, String category, String unit) {}
