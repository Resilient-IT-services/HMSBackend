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
    // 1. Create multiple medicines
    createMedicine("Alpha", "Generic A", "Manufacturer X", 10.0, 15.0);
    createMedicine("Beta", "Generic B", "Manufacturer Y", 20.0, 25.0);
    createMedicine("Gamma", "Generic A", "Manufacturer X", 30.0, 35.0);

    // 2. Filter by genericName=Generic A
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("genericName", "Generic A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2));

    // 3. Filter by manufacturer=Manufacturer Y
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("manufacturer", "Manufacturer Y"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].name").value("Beta"));

    // 4. Search by name
    mockMvc
        .perform(
            get("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("search", "Gamma"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].name").value("Gamma"));

    // 5. Sort by name desc
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

  private void createMedicine(
      String name,
      String genericName,
      String manufacturer,
      double purchasePrice,
      double sellingPrice)
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
            + "\"purchasePrice\": "
            + purchasePrice
            + ","
            + "\"sellingPrice\": "
            + sellingPrice
            + ","
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
            + "\"purchasePrice\": 10.0,"
            + "\"sellingPrice\": 15.0,"
            + "\"manufacturer\": \"Generic Pharma\""
            + "}";

    // 1. Create first time
    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(medicineJson))
        .andExpect(status().isOk());

    // 2. Create again (duplicate)
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
    // 1. Create
    String createRequest =
        "{"
            + "\"name\": \"Paracetamol\","
            + "\"genericName\": \"Acetaminophen\","
            + "\"composition\": \"500mg\","
            + "\"strength\": \"500mg\","
            + "\"form\": \"TABLET\","
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
        .andExpect(jsonPath("$.data.sellingPrice").value(5.0));

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
                        + "\"genericName\": \"Amoxicillin\","
                        + "\"composition\": \"Amoxicillin 500mg\","
                        + "\"strength\": \"500mg\","
                        + "\"form\": \"CAPSULE\","
                        + "\"manufacturer\": \"Generic Pharma\","
                        + "\"batchNumber\": \"AMOX-01\","
                        + "\"currentStock\": 50,"
                        + "\"purchasePrice\": 5.0,"
                        + "\"sellingPrice\": 10.0"
                        + "}"))
        .andExpect(status().isOk());

    // 2. List Stock
    mockMvc
        .perform(
            get("/api/v1/pharmacy/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].medicineName").value("Amoxicillin"))
        .andExpect(jsonPath("$.data.content[0].batchNumber").value("AMOX-01"))
        .andExpect(jsonPath("$.data.content[0].quantity").value(50));

    // 3. Filter Low Stock (Threshold is 20 in service)
    mockMvc
        .perform(
            get("/api/v1/pharmacy/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("status", "LOW_STOCK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }
}
