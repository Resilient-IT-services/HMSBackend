package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("BILL_TEST", "Bill Hospital");

    if (!userAccountRepository.existsByUsername("bill_admin")) {
      UserAccount hospitalAdmin = new UserAccount();
      hospitalAdmin.setUsername("bill_admin");
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
                        .content("{\"username\": \"bill_admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testBillingFlow() throws Exception {
    UUID patientId = testDataHelper.createTestPatient(testHospitalId).getId();

    // 1. Create Billing
    String billingRequest =
        String.format(
            "{\"patientId\": \"%s\", \"sourceType\": \"OPD\", \"status\": \"PENDING\", \"totalAmount\": 150.0, \"discount\": 10.0, \"paymentMethod\": \"CASH\", \"items\": [{\"itemName\": \"Consultation\", \"amount\": 100.0, \"quantity\": 1, \"subtotal\": 100.0}, {\"itemName\": \"Medicine\", \"amount\": 50.0, \"quantity\": 1, \"subtotal\": 50.0}]}",
            patientId);

    String response =
        mockMvc
            .perform(
                post("/api/v1/billing")
                    .session(session)
                    .header("X-Hospital-Id", testHospitalId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(billingRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalAmount").value(150.0))
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String billingId = com.jayway.jsonpath.JsonPath.read(response, "$.data.id");

    // 2. Update Status to PAID
    mockMvc
        .perform(
            put("/api/v1/billing/" + billingId + "/status")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .param("status", "PAID"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PAID"));
  }
}
