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
class PatientControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private HospitalRepository hospitalRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private String hospitalCode;
  private String adminUsername;

  @BeforeEach
  void setup() {
    hospitalCode = "PAT_TEST_" + UUID.randomUUID().toString().substring(0, 8);
    adminUsername = "pat_admin_" + UUID.randomUUID().toString().substring(0, 8);
    testHospitalId = testDataHelper.createHospitalIfMissing(hospitalCode, "Patient Test Hospital");

    UserAccount hospitalAdmin = new UserAccount();
    hospitalAdmin.setUsername(adminUsername);
    hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
    hospitalAdmin.setActive(true);

    UserContract assignment = new UserContract();
    assignment.setUser(hospitalAdmin);
    assignment.setHospitalId(testHospitalId);
    assignment.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
    hospitalAdmin.getContracts().add(assignment);

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
                            String.format(
                                "{ \"username\": \"%s\", \"password\": \"admin123\" }",
                                adminUsername)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

    // Fetch /me to get hospital list
    mockMvc
        .perform(get("/api/v1/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.memberships[0].hospitalCode").value(hospitalCode));

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

  @Test
  void testPatientListingWithFilteringAndSorting() throws Exception {
    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            String.format(
                                "{ \"username\": \"%s\", \"password\": \"admin123\" }",
                                adminUsername)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

    // 1. Setup multiple patients
    createPatient(session, "Alice Smith", "9000000001", "FEMALE", "1990-01-01");
    createPatient(session, "Bob Jones", "9000000002", "MALE", "1985-05-05");
    createPatient(session, "Charlie Brown", "9000000003", "MALE", "2000-10-10");

    // 2. Filter by gender=FEMALE
    mockMvc.perform(get("/api/v1/patients")
            .session(session)
            .header("X-Hospital-Id", testHospitalId.toString())
            .param("gender", "FEMALE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].fullName").value("Alice Smith"));

    // 3. Search by name
    mockMvc.perform(get("/api/v1/patients")
            .session(session)
            .header("X-Hospital-Id", testHospitalId.toString())
            .param("search", "Jones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].fullName").value("Bob Jones"));

    // 4. Sort by fullName asc
    mockMvc.perform(get("/api/v1/patients")
            .session(session)
            .header("X-Hospital-Id", testHospitalId.toString())
            .param("sort", "fullName,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].fullName").value("Alice Smith"))
        .andExpect(jsonPath("$.data.content[1].fullName").value("Bob Jones"))
        .andExpect(jsonPath("$.data.content[2].fullName").value("Charlie Brown"));
  }

  private void createPatient(MockHttpSession session, String fullName, String phone, String gender, String dob) throws Exception {
    String patientJson = String.format(
        """
            {
              "fullName": "%s",
              "phone": "%s",
              "gender": "%s",
              "dob": "%s"
            }
            """, fullName, phone, gender, dob);

    mockMvc
        .perform(
            post("/api/v1/patients")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(patientJson))
        .andExpect(status().isOk());
  }

  @Test
  void duplicatePatientRegistrationFails() throws Exception {
    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            String.format(
                                "{ \"username\": \"%s\", \"password\": \"admin123\" }",
                                adminUsername)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

    String patientJson =
        """
            {
              "fullName": "Duplicate Patient",
              "phone": "1234567890",
              "gender": "MALE",
              "dob": "1990-01-01"
            }
            """;

    // Register first time
    mockMvc
        .perform(
            post("/api/v1/patients")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(patientJson))
        .andExpect(status().isOk());

    // Register second time with same name and phone
    mockMvc
        .perform(
            post("/api/v1/patients")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(patientJson))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
  }
}
