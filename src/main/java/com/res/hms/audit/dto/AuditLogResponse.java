package com.res.hms.audit.dto;

import com.res.hms.domain.AuditAction;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    String entityName,
    UUID entityId,
    AuditAction action,
    UUID performedBy,
    AuditDetail details,
    LocalDateTime createdAt) {}
