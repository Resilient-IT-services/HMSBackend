package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
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
class GlobalSearchIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private HospitalRepository hospitalRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;

  @BeforeEach
  void setupData() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("SEARCH_TEST", "Search Test Hospital");

    if (!userAccountRepository.existsByUsername("search_admin")) {
      UserAccount admin = new UserAccount();
      admin.setUsername("search_admin");
      admin.setPasswordHash(passwordEncoder.encode("admin123"));
      admin.setActive(true);
      admin.setFullName("Global Admin");

      UserContract assignment = new UserContract();
      assignment.setUser(admin);
      assignment.setHospitalId(testHospitalId);
      assignment.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
      admin.getContracts().add(assignment);

      userAccountRepository.save(admin);
    }

    // Create a patient for searching
    mockMvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\": \"search_admin\", \"password\": \"admin123\"}"));
  }

  @Test
  void testGlobalSearch() throws Exception {
    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"search_admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

    // Create a patient to search for
    mockMvc
        .perform(
            post("/api/v1/patients")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {
              "fullName": "Searchable Patient",
              "gender": "MALE",
              "dob": "1990-01-01",
              "phone": "1234567890",
              "address": "Test City"
            }
            """))
        .andExpect(status().isOk());

    // Search for the patient
    mockMvc
        .perform(
            get("/api/v1/search")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("query", "Searchable"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(
            jsonPath("$.data.content[?(@.type=='PATIENT' && @.title=='Searchable Patient')]")
                .exists());

    // Search for the admin user
    mockMvc
        .perform(
            get("/api/v1/search")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("query", "Global"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.content[?(@.type=='USER' && @.title=='Global Admin')]").exists());
  }
}
