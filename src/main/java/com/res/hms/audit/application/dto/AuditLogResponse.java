package com.res.hms.audit.application.dto;

import com.res.hms.audit.application.dto.AuditDetail;
import com.res.hms.audit.domain.AuditAction;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    String entityName,
    UUID entityId,
    AuditAction action,
    UUID performedBy,
    AuditDetail details) {}
