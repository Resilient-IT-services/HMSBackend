package com.res.hms.common.audit;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.tenant.HospitalContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    String details =
        String.format("%s %s with ID %s", auditable.action(), auditable.entity(), entityId);

    auditLogService.record(hospitalId, auditable.entity(), entityId, auditable.action(), details);
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
