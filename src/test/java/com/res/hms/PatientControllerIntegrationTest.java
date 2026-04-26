package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
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
class PatientControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private HospitalRepository hospitalRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;

  @BeforeEach
  void ensureHospitalAdminExists() {
    testHospitalId = testDataHelper.createHospitalIfMissing("TEST", "Test Hospital");

    if (userAccountRepository.existsByUsername("hospital_admin")) {
      return;
    }

    UserAccount hospitalAdmin = new UserAccount();
    hospitalAdmin.setUsername("hospital_admin");
    hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
    hospitalAdmin.setActive(true);

    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(hospitalAdmin);
    assignment.setHospitalId(testHospitalId);
    assignment.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
    hospitalAdmin.getAssignments().add(assignment);

    userAccountRepository.save(hospitalAdmin);
  }

  @Test
  void patientEndpointsRequireAuthenticationAndUseSessionCookie() throws Exception {
    mockMvc.perform(get("/api/v1/patients")).andExpect(status().isUnauthorized());

    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                                {
                                  "username": "hospital_admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

    // Fetch /me to get hospital list
    mockMvc
        .perform(get("/api/v1/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.memberships[0].hospitalCode").value("TEST"));

    // Try to access patients without selecting hospital -> should fail with 400
    mockMvc
        .perform(get("/api/v1/patients").session(session))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.message").value("Please select a hospital first"));

    // Now it should work by sending X-Hospital-Id header
    mockMvc
        .perform(
            post("/api/v1/patients")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "fullName": "Asha Nair",
                                  "gender": "FEMALE",
                                  "dob": "1992-08-14",
                                  "phone": "9999999999",
                                  "address": "Kochi"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fullName").value("Asha Nair"));

    mockMvc
        .perform(
            get("/api/v1/patients")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].fullName").value("Asha Nair"));
  }
}
