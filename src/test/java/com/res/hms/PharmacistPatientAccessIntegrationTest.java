package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PharmacistPatientAccessIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;

  @BeforeEach
  void setup() {
    testHospitalId = testDataHelper.createHospitalIfMissing("PHARMA_TEST", "Pharmacist Test Hospital");

    if (!userAccountRepository.existsByUsername("test_pharmacist")) {
      UserAccount pharmacist = new UserAccount();
      pharmacist.setUsername("test_pharmacist");
      pharmacist.setPasswordHash(passwordEncoder.encode("pharma123"));
      pharmacist.setActive(true);

      UserContract assignment = new UserContract();
      assignment.setUser(pharmacist);
      assignment.setHospitalId(testHospitalId);
      assignment.setRole(roleRepository.findByCode(RoleCode.PHARMACIST).orElseThrow());
      pharmacist.getContracts().add(assignment);

      userAccountRepository.save(pharmacist);
    }
  }

  @Test
  void pharmacistCanAccessPatientListAndSummary() throws Exception {
    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                                {
                                  "username": "test_pharmacist",
                                  "password": "pharma123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

    // Test Patient List
    mockMvc
        .perform(
            get("/api/v1/patients")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk());

    // Test Patient Summary List
    mockMvc
        .perform(
            get("/api/v1/patients/summary")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk());
  }
}
