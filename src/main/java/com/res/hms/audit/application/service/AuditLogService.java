package com.res.hms.audit.application.service;

import com.res.hms.audit.application.dto.AuditDetail;
import com.res.hms.audit.application.dto.AuditLogResponse;
import com.res.hms.audit.domain.AuditAction;
import com.res.hms.audit.domain.AuditLog;
import com.res.hms.audit.domain.DomainEntity;
import com.res.hms.audit.infrastructure.repository.AuditLogRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;
  private final HospitalContext hospitalContext;

  public void record(
      UUID hospitalId,
      DomainEntity entityName,
      UUID entityId,
      AuditAction action,
      AuditDetail details) {
    AuditLog auditLog = new AuditLog();
    auditLog.setHospitalId(hospitalId);
    auditLog.setEntityName(entityName);
    auditLog.setEntityId(entityId);
    auditLog.setAction(action);
    auditLog.setPerformedBy(hospitalContext.currentUserIdOrNull());
    auditLog.setDetails(details);
    auditLogRepository.save(auditLog);
  }

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> list(
      org.springframework.data.domain.Pageable pageable,
      String search,
      java.util.Map<String, String> filters) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();

    if (hospitalId == null && !hospitalContext.isSuperAdmin()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }

    com.res.hms.core.util.SpecificationBuilder<AuditLog> builder =
        new com.res.hms.core.util.SpecificationBuilder<>(
            filters, search, java.util.List.of("entityName", "action"));

    org.springframework.data.jpa.domain.Specification<AuditLog> spec = builder.build();
    org.springframework.data.jpa.domain.Specification<AuditLog> fixedSpec =
        (root, query, cb) -> {
          if (hospitalId != null) {
            return cb.equal(root.get("hospitalId"), hospitalId);
          }
          return null;
        };

    return PageResponse.from(
        auditLogRepository
            .findAll(
                org.springframework.data.jpa.domain.Specification.where(spec).and(fixedSpec),
                pageable)
            .map(this::toResponse));
  }

  private AuditLogResponse toResponse(AuditLog auditLog) {
    return new AuditLogResponse(
        auditLog.getId(),
        auditLog.getEntityName() != null ? auditLog.getEntityName().name() : null,
        auditLog.getEntityId(),
        auditLog.getAction(),
        auditLog.getPerformedBy(),
        auditLog.getDetails());
  }
}
