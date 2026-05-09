package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.UUID;

import com.res.hms.audit.application.dto.AuditDetail;
import com.res.hms.audit.application.service.AuditLogService;
import com.res.hms.audit.domain.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditLogService auditLogService;
  @Autowired private TestDataHelper testDataHelper;

  private UUID hospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("AUDIT_TEST", "Audit Hospital");

    // Login as admin
    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testListAuditLogs() throws Exception {
    // Manually record some logs
    auditLogService.record(
        hospitalId, "test", UUID.randomUUID(), AuditAction.CREATE, new AuditDetail());
    auditLogService.record(
        hospitalId, "test", UUID.randomUUID(), AuditAction.UPDATE, new AuditDetail());

    mockMvc
        .perform(
            get("/api/v1/audit-logs")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.content.length()")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
  }

  @Test
  void testListAuditLogsAsSuperAdminWorksWithoutHospital() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs").session(session)).andExpect(status().isOk());
  }
}
