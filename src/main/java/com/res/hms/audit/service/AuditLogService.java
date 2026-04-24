package com.res.hms.audit.service;

import com.res.hms.audit.dto.AuditLogResponse;
import com.res.hms.audit.repository.AuditLogRepository;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.AuditLog;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;
  private final HospitalContext hospitalContext;

  public void record(
      UUID hospitalId, String entityName, UUID entityId, String action, String details) {
    AuditLog auditLog = new AuditLog();
    auditLog.setHospitalId(hospitalId);
    auditLog.setEntityName(entityName);
    auditLog.setEntityId(entityId);
    auditLog.setAction(action);
    auditLog.setPerformedBy(hospitalContext.currentUserIdOrNull());
    auditLog.setDetails(details);
    auditLogRepository.save(auditLog);
  }

  public PageResponse<AuditLogResponse> list(int page, int size) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    Page<AuditLog> auditLogs;

    if (hospitalId != null) {
      auditLogs =
          auditLogRepository.findAllByHospitalIdOrderByCreatedAtDesc(
              hospitalId, PageRequest.of(page, size));
    } else if (hospitalContext.isSuperAdmin()) {
      auditLogs = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    } else {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }

    return PageResponse.from(auditLogs.map(this::toResponse));
  }

  private AuditLogResponse toResponse(AuditLog auditLog) {
    return new AuditLogResponse(
        auditLog.getId(),
        auditLog.getEntityName(),
        auditLog.getEntityId(),
        auditLog.getAction(),
        auditLog.getPerformedBy(),
        auditLog.getDetails(),
        auditLog.getCreatedAt());
  }
}
