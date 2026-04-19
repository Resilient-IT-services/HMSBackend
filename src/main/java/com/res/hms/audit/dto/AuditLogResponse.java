package com.res.hms.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    String entityName,
    Long entityId,
    String action,
    Long performedBy,
    String details,
    LocalDateTime createdAt) {}
