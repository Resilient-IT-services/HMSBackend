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

    String username = "med_admin_" + UUID.randomUUID().toString().substring(0, 8);
    UserAccount hospitalAdmin = new UserAccount();
    hospitalAdmin.setUsername(username);
    hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
    hospitalAdmin.setActive(true);
    hospitalAdmin.setSuperuser(true);

    UserContract assignment = new UserContract();
    assignment.setUser(hospitalAdmin);
    assignment.setHospitalId(testHospitalId);
    assignment.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
    hospitalAdmin.getContracts().add(assignment);

    userAccountRepository.save(hospitalAdmin);

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"username\": \"" + username + "\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testMedicineListingWithFilteringAndSorting() throws Exception {
    createMedicine("Alpha", "Generic A", "Manufacturer X");
    createMedicine("Beta", "Generic B", "Manufacturer Y");
    createMedicine("Gamma", "Generic A", "Manufacturer X");

    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("genericName", "Generic A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2));

    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("manufacturer", "Manufacturer Y"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].name").value("Beta"));

    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("search", "Gamma"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].name").value("Gamma"));

    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("sort", "name,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("Gamma"))
        .andExpect(jsonPath("$.data.content[1].name").value("Beta"))
        .andExpect(jsonPath("$.data.content[2].name").value("Alpha"));
  }

  private void createMedicine(String name, String genericName, String manufacturer)
      throws Exception {
    String medicineJson =
        "{"
            + "\"name\": \""
            + name
            + "\","
            + "\"genericName\": \""
            + genericName
            + "\","
            + "\"composition\": \"test\","
            + "\"strength\": \"test\","
            + "\"form\": \"TABLET\","
            + "\"manufacturer\": \""
            + manufacturer
            + "\""
            + "}";

    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(medicineJson))
        .andExpect(status().isOk());
  }

  @Test
  void testDuplicateMedicineValidation() throws Exception {
    String medicineJson =
        "{"
            + "\"name\": \"Azithromycin\","
            + "\"genericName\": \"Azithromycin\","
            + "\"composition\": \"Azithromycin 250mg\","
            + "\"strength\": \"250mg\","
            + "\"form\": \"TABLET\","
            + "\"manufacturer\": \"Generic Pharma\""
            + "}";

    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(medicineJson))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(medicineJson))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("DUPLICATE_MEDICINE"));
  }

  @Test
  void testMedicineCRUD() throws Exception {
    String createRequest =
        "{"
            + "\"name\": \"Paracetamol\","
            + "\"genericName\": \"Acetaminophen\","
            + "\"composition\": \"500mg\","
            + "\"strength\": \"500mg\","
            + "\"form\": \"TABLET\","
            + "\"category\": \"Analgesic\","
            + "\"manufacturer\": \"GSK\","
            + "\"thresholdQuantity\": 20,"
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
        .andExpect(jsonPath("$.data.genericName").value("Acetaminophen"));

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
    void testSearchByHsnCode() throws Exception {
    String payload =
        "{"
            + "\"name\":\"HSNTest\","
            + "\"genericName\":\"HSNTest\","
            + "\"composition\":\"HSNTest\","
            + "\"strength\":\"100mg\","
            + "\"form\":\"TABLET\","
            + "\"manufacturer\":\"HSNTest\","
            + "\"hsnCode\":\"HSN12345\""
            + "}";

    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    // Partial match, case-insensitive
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("search", "hsn123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("HSNTest"))
        .andExpect(jsonPath("$.data.content[0].hsnCode").value("HSN12345"));
  }

  @Test
  void testCreateMedicineWithoutPrices() throws Exception {
    String payload =
        "{"
            + "\"name\":\"Zmed123\","
            + "\"genericName\":\"Zmed123\","
            + "\"composition\":\"Zmed123\","
            + "\"strength\":\"100mg\","
            + "\"form\":\"TABLET\","
            + "\"manufacturer\":\"Zmed123\","
            + "\"scheduleType\":\"OTC\","
            + "\"category\":\"General\","
            + "\"thresholdQuantity\":10,"
            + "\"hsnCode\":\"Zmed123\""
            + "}";

    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Zmed123"))
        .andExpect(jsonPath("$.data.id").exists());
  }
}
