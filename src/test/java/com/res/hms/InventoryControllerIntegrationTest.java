package com.res.hms;

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
class InventoryControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("TEST_INV", "Test Hospital Inv");

    if (!userAccountRepository.existsByUsername("inv_admin")) {
      UserAccount hospitalAdmin = new UserAccount();
      hospitalAdmin.setUsername("inv_admin");
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
                        .content("{\"username\": \"inv_admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testInventoryFlow() throws Exception {
    // 1. Create Item
    String itemResponse =
        mockMvc
            .perform(
                post("/api/v1/inventory/items")
                    .session(session)
                    .header("X-Hospital-Id", testHospitalId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\": \"Syringe\", \"category\": \"Consumables\", \"unit\": \"pcs\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Syringe"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String itemId = com.jayway.jsonpath.JsonPath.read(itemResponse, "$.data.id");

    // 2. Stock In
    mockMvc
        .perform(
            post("/api/v1/inventory/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"itemId\": \"%s\", \"type\": \"STOCK_IN\", \"quantity\": 100, \"reference\": \"PO-001\"}",
                        itemId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.quantity").value(100.0));

    // 3. Stock Out
    mockMvc
        .perform(
            post("/api/v1/inventory/stock")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"itemId\": \"%s\", \"type\": \"STOCK_OUT\", \"quantity\": 20, \"reference\": \"Used\"}",
                        itemId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.quantity").value(80.0));
  }
}
