package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("REPORT_TEST", "Report Hospital");

    if (!userAccountRepository.existsByUsername("report_admin")) {
      UserAccount hospitalAdmin = new UserAccount();
      hospitalAdmin.setUsername("report_admin");
      hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
      hospitalAdmin.setActive(true);

      UserContract assignment = new UserContract();
      assignment.setUser(hospitalAdmin);
      assignment.setHospitalId(testHospitalId);
      assignment.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
      hospitalAdmin.getContracts().add(assignment);

      userAccountRepository.save(hospitalAdmin);
    }

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"report_admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testReportPreview() throws Exception {
    String now = java.time.Instant.now().toString();

    mockMvc
        .perform(
            get("/api/v1/reports/preview")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("type", "DOCTOR_REVENUE")
                .param("from", now)
                .param("to", now))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());

    mockMvc
        .perform(
            get("/api/v1/reports/preview")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("type", "PAYMENT_SUMMARY")
                .param("from", now)
                .param("to", now))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void testReportExport() throws Exception {
    String now = java.time.Instant.now().toString();

    mockMvc
        .perform(
            get("/api/v1/reports/export/excel")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("type", "DOCTOR_REVENUE")
                .param("from", now)
                .param("to", now))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              String contentType = result.getResponse().getContentType();
              assert contentType != null && contentType.contains("spreadsheetml");
            });

    mockMvc
        .perform(
            get("/api/v1/reports/export/pdf")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("type", "PAYMENT_SUMMARY")
                .param("from", now)
                .param("to", now))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              String contentType = result.getResponse().getContentType();
              assert contentType != null && contentType.equals("application/pdf");
            });
  }
}
