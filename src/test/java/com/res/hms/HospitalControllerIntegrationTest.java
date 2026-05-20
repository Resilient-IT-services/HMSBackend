package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HospitalControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestDataHelper testDataHelper;

  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    // Login as super admin
    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testListHospitalsWithFilters() throws Exception {
    testDataHelper.createHospitalIfMissing("HOSP1", "General Hospital");
    testDataHelper.createHospitalIfMissing("HOSP2", "City Clinic");

    // 1. List all
    mockMvc
        .perform(get("/api/v1/hospitals").session(session))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.content.length()")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

    // 2. Filter by name
    mockMvc
        .perform(get("/api/v1/hospitals").session(session).param("name", "General"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("General Hospital"));

    // 3. Filter by status
    mockMvc
        .perform(get("/api/v1/hospitals").session(session).param("status", "ACTIVE"))
        .andExpect(status().isOk());

    // 4. Invalid status
    mockMvc
        .perform(get("/api/v1/hospitals").session(session).param("status", "INVALID"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testGetHospitalById() throws Exception {
    UUID hId = testDataHelper.createHospitalIfMissing("HOSP3", "Specific Hospital");

    mockMvc
        .perform(get("/api/v1/hospitals/" + hId).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Specific Hospital"));

    mockMvc
        .perform(get("/api/v1/hospitals/" + UUID.randomUUID()).session(session))
        .andExpect(status().isNotFound());
  }

  @Test
  void testCreateHospitalValidation() throws Exception {
    // 1. Missing timezone should fail
    String hospitalJson = "{\"name\": \"Validation Hospital\", \"code\": \"VAL-HOSP\"}";
    mockMvc
        .perform(
            post("/api/v1/hospitals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(hospitalJson))
        .andExpect(status().isBadRequest());

    // 2. Complete data should succeed
    hospitalJson =
        "{\"name\": \"Validation Hospital\", \"code\": \"VAL-HOSP\", \"timezone\": \"Asia/Kolkata\"}";
    mockMvc
        .perform(
            post("/api/v1/hospitals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(hospitalJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"));
  }
}
