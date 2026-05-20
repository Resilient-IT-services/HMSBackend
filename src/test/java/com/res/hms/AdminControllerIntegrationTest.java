package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AdminControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("ADMIN_TEST", "Admin Hospital");

    if (!userAccountRepository.existsByUsername("super_admin")) {
      UserAccount superAdmin = new UserAccount();
      superAdmin.setUsername("super_admin");
      superAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
      superAdmin.setSuperuser(true);
      superAdmin.setActive(true);
      userAccountRepository.save(superAdmin);
    }

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"super_admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testVitalRanges() throws Exception {
    mockMvc.perform(get("/api/v1/admin/vital-ranges").session(session)).andExpect(status().isOk());

    String request = "{\"vitalType\": \"HEART_RATE\", \"minValue\": 70, \"maxValue\": 110}";
    mockMvc
        .perform(
            put("/api/v1/admin/vital-ranges/HEART_RATE")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.minValue").value(70.0));
  }
}
