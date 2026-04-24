package com.res.hms.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.audit.dto.AuditDetail;
import com.res.hms.audit.dto.AuditLogResponse;
import com.res.hms.audit.repository.AuditLogRepository;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Mock private HospitalContext hospitalContext;

  @InjectMocks private AuditLogService auditLogService;

  private final UUID hospitalUuid = UUID.randomUUID();
  private final UUID patientUuid = UUID.randomUUID();

  @Test
  void recordSavesAuditLog() {
    AuditDetail detail = AuditDetail.builder().after(Map.of("msg", "created")).build();
    auditLogService.record(hospitalUuid, "patient", patientUuid, "CREATE", detail);

    verify(auditLogRepository).save(any(AuditLog.class));
  }

  @Test
  void listReturnsMappedResponses() {
    AuditLog auditLog = new AuditLog();
    auditLog.setDetails(AuditDetail.builder().after(Map.of("msg", "created")).build());
    auditLog.setCreatedAt(LocalDateTime.now());

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalUuid);
    when(auditLogRepository.findAllByHospitalIdOrderByCreatedAtDesc(
            hospitalUuid, PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(auditLog)));

    var response = auditLogService.list(0, 10);

    assertEquals(1, response.content().size());
  }
}
