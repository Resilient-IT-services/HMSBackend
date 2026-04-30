package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
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
class MedicineControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("TEST_MED", "Test Hospital Med");

    if (!userAccountRepository.existsByUsername("med_admin")) {
      UserAccount hospitalAdmin = new UserAccount();
      hospitalAdmin.setUsername("med_admin");
      hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
      hospitalAdmin.setActive(true);

      UserRoleAssignment assignment = new UserRoleAssignment();
      assignment.setUser(hospitalAdmin);
      assignment.setHospitalId(testHospitalId);
      assignment.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
      hospitalAdmin.getAssignments().add(assignment);

      userAccountRepository.save(hospitalAdmin);
    }

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"med_admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testMedicineCRUD() throws Exception {
    // 1. Create
    String createRequest =
        "{"
            + "\"name\": \"Paracetamol\","
            + "\"genericName\": \"Acetaminophen\","
            + "\"composition\": \"500mg\","
            + "\"category\": \"Analgesic\","
            + "\"manufacturer\": \"GSK\","
            + "\"batchNumber\": \"BATCH-001\","
            + "\"currentStock\": 100,"
            + "\"reorderLevel\": 20,"
            + "\"purchasePrice\": 2.50,"
            + "\"sellingPrice\": 5.00,"
            + "\"taxPercentage\": 12.5,"
            + "\"temperature\": \"Room Temp\","
            + "\"humidity\": \"Dry\","
            + "\"specialNotes\": \"Keep away from light\""
            + "}";

    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Paracetamol"))
        .andExpect(jsonPath("$.data.genericName").value("Acetaminophen"))
        .andExpect(jsonPath("$.data.category").value("Analgesic"))
        .andExpect(jsonPath("$.data.sellingPrice").value(5.0))
        .andExpect(jsonPath("$.data.specialNotes").value("Keep away from light"));

    // 2. List
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("Paracetamol"));

    // 3. Search
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("search", "Para"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("Paracetamol"));
  }

  @Test
  void testPharmacyStockListing() throws Exception {
    // 1. Create Medicine with Stock
    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"name\": \"Amoxicillin\","
                        + "\"batchNumber\": \"AMOX-01\","
                        + "\"currentStock\": 50,"
                        + "\"sellingPrice\": 10.0"
                        + "}"))
        .andExpect(status().isOk());

    // 2. List Stock
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].medicineName").value("Amoxicillin"))
        .andExpect(jsonPath("$.data.content[0].batchNumber").value("AMOX-01"))
        .andExpect(jsonPath("$.data.content[0].quantity").value(50));

    // 3. Filter Low Stock (Threshold is 20 in service)
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("lowStock", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }
}
