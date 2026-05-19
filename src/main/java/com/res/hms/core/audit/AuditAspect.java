package com.res.hms.core.audit;

import com.res.hms.audit.application.dto.AuditDetail;
import com.res.hms.audit.application.service.AuditLogService;
import com.res.hms.audit.domain.AuditAction;
import com.res.hms.core.tenant.HospitalContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

  private final AuditLogService auditLogService;
  private final HospitalContext hospitalContext;

  @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
  public void logAudit(JoinPoint joinPoint, Auditable auditable, Object result) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    UUID entityId = extractId(result);

    AuditDetail details =
        AuditDetail.builder().after(result != null ? Map.of("data", result) : null).build();

    auditLogService.record(
        hospitalId, com.res.hms.audit.domain.DomainEntity.valueOf(auditable.entity()), entityId, AuditAction.valueOf(auditable.action()), details);
  }

  private UUID extractId(Object result) {
    if (result == null) {
      return null;
    }
    try {
      Method getId = result.getClass().getMethod("id");
      return (UUID) getId.invoke(result);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      try {
        Method getId = result.getClass().getMethod("getId");
        return (UUID) getId.invoke(result);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
        return null;
      }
    }
  }
}
