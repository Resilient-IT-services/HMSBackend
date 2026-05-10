package com.res.hms.audit.application.dto;

import java.util.UUID;

import com.res.hms.audit.domain.AuditAction;

public record AuditLogResponse(
    UUID id,
    String entityName,
    UUID entityId,
    AuditAction action,
    UUID performedBy,
    AuditDetail details) {}
