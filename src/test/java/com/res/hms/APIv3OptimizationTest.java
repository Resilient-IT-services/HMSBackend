package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.iam.domain.UserAccount;
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
class APIv3OptimizationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession superAdminSession;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("OPT_TEST", "Optimization Hospital");

    if (!userAccountRepository.existsByUsername("super_admin_opt")) {
      UserAccount superAdmin = new UserAccount();
      superAdmin.setUsername("super_admin_opt");
      superAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
      superAdmin.setSuperuser(true);
      superAdmin.setActive(true);
      superAdmin.setFullName("Super Admin");
      userAccountRepository.save(superAdmin);
    }

    superAdminSession =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"super_admin_opt\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testLightweightSelectionEndpoints() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/staff/doctors/selection")
                .session(superAdminSession)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());

    mockMvc
        .perform(get("/api/v1/hospitals/selection").session(superAdminSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void testAdminStatsEndpoint() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/stats").session(superAdminSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalUsers").isNumber())
        .andExpect(jsonPath("$.data.totalHospitals").isNumber());
  }

  @Test
  void testAppointmentPatientFiltering() throws Exception {
    UUID patientId = UUID.randomUUID(); // Doesn't need to exist for empty result check
    mockMvc
        .perform(
            get("/api/v1/appointments?patientId=" + patientId)
                .session(superAdminSession)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
  }
}
