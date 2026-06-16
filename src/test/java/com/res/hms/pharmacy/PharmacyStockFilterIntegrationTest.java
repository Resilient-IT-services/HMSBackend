package com.res.hms.pharmacy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.TestDataHelper;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.pharmacy.application.dto.StockRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class PharmacyStockFilterIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private ObjectMapper objectMapper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    testHospitalId =
        testDataHelper.createHospitalIfMissing(
            "STOCK_" + uniqueId, "Stock Test Hospital " + uniqueId);

    String username = "stock_admin_" + uniqueId;
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
  void testLowStockFilter() throws Exception {
    // 1. Create Medicine with threshold 10
    UUID medId = createMedicine("LowStockMed", 10);

    // 2. Add normal stock (20 units)
    addStock(medId, "BATCH001", 20, LocalDate.now().plusYears(1));

    // 3. Add low stock (5 units)
    addStock(medId, "BATCH002", 5, LocalDate.now().plusYears(1));

    // 4. Verify all stock
    mockMvc
        .perform(
            get("/api/v1/pharmacy/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2));

    // 5. Verify low stock filter (SHOULD FAIL currently because it expects status=LOW_STOCK)
    mockMvc
        .perform(
            get("/api/v1/pharmacy/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("lowStock", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].batchNumber").value("BATCH002"));
  }

  @Test
  void testExpiredFilter() throws Exception {
    UUID medId = createMedicine("ExpiredMed", 10);

    // Not expired
    addStock(medId, "NOT_EXPIRED", 20, LocalDate.now().plusYears(1));

    // Expired
    addStock(medId, "EXPIRED", 20, LocalDate.now().minusDays(1));

    // Verify expired filter
    mockMvc
        .perform(
            get("/api/v1/pharmacy/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("expired", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].batchNumber").value("EXPIRED"));
  }

  @Test
  void testExpiringSoonFilter() throws Exception {
    UUID medId = createMedicine("SoonMed", 10);

    // Far expiry
    addStock(medId, "FAR", 20, LocalDate.now().plusMonths(6));

    // Expiring soon (within 30 days)
    addStock(medId, "SOON", 20, LocalDate.now().plusDays(15));

    // Verify expiring soon filter
    mockMvc
        .perform(
            get("/api/v1/pharmacy/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("expiringSoon", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].batchNumber").value("SOON"));
  }

  private UUID createMedicine(String name, int threshold) throws Exception {
    String medicineJson =
        String.format(
            "{"
                + "\"name\": \"%s\","
                + "\"genericName\": \"Generic\","
                + "\"composition\": \"test\","
                + "\"strength\": \"test\","
                + "\"form\": \"TABLET\","
                + "\"manufacturer\": \"Test\","
                + "\"thresholdQuantity\": %d"
                + "}",
            name, threshold);

    String response =
        mockMvc
            .perform(
                post("/api/v1/pharmacy/medicines")
                    .session(session)
                    .header("X-Hospital-Id", testHospitalId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(medicineJson))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("data").get("id").asText());
  }

  private void addStock(UUID medId, String batch, int qty, LocalDate expiry) throws Exception {
    StockRequest request =
        StockRequest.builder()
            .medicineId(medId)
            .batchNumber(batch)
            .quantity(qty)
            .expiryDate(expiry)
            .purchasePrice(BigDecimal.TEN)
            .sellingPrice(BigDecimal.valueOf(15))
            .build();

    mockMvc
        .perform(
            post("/api/v1/pharmacy/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}
