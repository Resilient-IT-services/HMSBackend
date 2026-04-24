package com.res.hms.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.audit.repository.AuditLogRepository;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Mock private HospitalContext hospitalContext;
  private final UUID patientUuid = UUID.nameUUIDFromBytes("10".getBytes());
  private final UUID hospitalUuid = UUID.nameUUIDFromBytes("4".getBytes());
  private final UUID currentUserID = UUID.nameUUIDFromBytes("1".getBytes());
  private final UUID auditLogUUID = UUID.nameUUIDFromBytes("500".getBytes());
  @InjectMocks private AuditLogService auditLogService;

  @Test
  void recordStoresAuditEntryWithCurrentUser() {
    when(hospitalContext.currentUserIdOrNull()).thenReturn(currentUserID);

    auditLogService.record(hospitalUuid, "patient", patientUuid, "CREATE", "created");

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertEquals(currentUserID, captor.getValue().getPerformedBy());
    assertEquals("patient", captor.getValue().getEntityName());
  }

  @Test
  void listReturnsMappedAuditLogs() {
    AuditLog auditLog = new AuditLog();
    auditLog.setId(auditLogUUID);
    auditLog.setEntityName("patient");
    auditLog.setEntityId(patientUuid);
    auditLog.setAction("CREATE");
    auditLog.setPerformedBy(currentUserID);
    auditLog.setDetails("created");
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalUuid);
    when(auditLogRepository.findAllByHospitalIdOrderByCreatedAtDesc(
            eq(hospitalUuid), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(auditLog)));

    var response = auditLogService.list(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("patient", response.content().getFirst().entityName());
  }
}
