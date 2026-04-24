package com.res.hms.audit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    String entityName,
    UUID entityId,
    String action,
    UUID performedBy,
    String details,
    LocalDateTime createdAt) {}
