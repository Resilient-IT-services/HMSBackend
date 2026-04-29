package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class DoctorManagementIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TestDataHelper testDataHelper;

  private MockHttpSession session;
  private UUID hospitalId;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("DOC_MGMT", "Doc Mgmt Hospital");

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
  void testDoctorFullCycle() throws Exception {
    // 1. Create Doctor with initial schedule
    String userJson =
        String.format(
            "{\"username\":\"doc_lifecycle\",\"password\":\"password123\",\"fullName\":\"Lifecycle Doctor\",\"gender\":\"MALE\",\"dob\":\"1980-01-01\",\"doctorInfo\":{\"specialization\":\"Cardiology\",\"licenseNumber\":\"DOC-LIFE-001\"},\"memberships\":[{\"hospitalId\":\"%s\",\"roleAssignments\":[{\"roleCode\":\"DOCTOR\",\"schedules\":[{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"slotDurationMinutes\":30}]}]}]}",
            hospitalId);

    String userResponse =
        mockMvc
            .perform(
                post("/api/v1/users")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(userJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("doc_lifecycle"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID userId =
        UUID.fromString(objectMapper.readTree(userResponse).path("data").path("id").asText());
    UUID doctorId =
        UUID.fromString(
            objectMapper
                .readTree(userResponse)
                .path("data")
                .path("doctorInfo")
                .path("id")
                .asText());

    // 2. Update Doctor: Modify schedule and add exception
    String updateJson =
        String.format(
            "{\"username\":\"doc_lifecycle\",\"fullName\":\"Updated Lifecycle Doctor\",\"gender\":\"MALE\",\"dob\":\"1980-01-01\",\"memberships\":[{\"hospitalId\":\"%s\",\"roleAssignments\":[{\"roleCode\":\"DOCTOR\",\"doctorDetails\":{\"specialization\":\"Interventional Cardiology\"},\"schedules\":[{\"dayOfWeek\":1,\"startTime\":\"10:00\",\"endTime\":\"13:00\",\"slotDurationMinutes\":30},{\"dayOfWeek\":3,\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"slotDurationMinutes\":30}],\"availabilityExceptions\":[{\"date\":\"2026-05-01\",\"type\":\"FULL_DAY_OFF\",\"note\":\"Holiday\"},{\"date\":\"2026-05-02\",\"type\":\"MODIFIED\",\"startTime\":\"14:00\",\"endTime\":\"16:00\",\"slotDurationMinutes\":20,\"note\":\"Extra hours\"}]}]}]}",
            hospitalId);

    mockMvc
        .perform(
            put("/api/v1/users/" + userId)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fullName").value("Updated Lifecycle Doctor"));

    // 3. Verify availability reflects the changes
    // Monday (2026-05-04 is a Monday)
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", "2026-05-04"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions.length()").value(1))
        .andExpect(jsonPath("$.data.sessions[0].startTime").value("10:00"));

    // Wednesday (2026-05-06 is a Wednesday)
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", "2026-05-06"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions.length()").value(1))
        .andExpect(jsonPath("$.data.sessions[0].startTime").value("09:00"));

    // Exception: Full day off (2026-05-01)
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", "2026-05-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions.length()").value(0));

    // Exception: Modified (2026-05-02)
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", "2026-05-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions.length()").value(1))
        .andExpect(jsonPath("$.data.sessions[0].startTime").value("14:00"))
        .andExpect(jsonPath("$.data.sessions[0].endTime").value("16:00"));
  }

  @Test
  void createDoctorWithoutLicenseNumberShouldFail() throws Exception {
    String userJson =
        String.format(
            "{\"username\":\"doc_no_license\",\"password\":\"password123\",\"fullName\":\"No License Doctor\",\"gender\":\"MALE\",\"dob\":\"1980-01-01\",\"doctorInfo\":{\"specialization\":\"Cardiology\"},\"memberships\":[{\"hospitalId\":\"%s\",\"roleAssignments\":[{\"roleCode\":\"DOCTOR\"}]}]}",
            hospitalId);

    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.message").value("Doctor license number is required"));
  }

  @Test
  void createDoctorWithExistingLicenseNumberShouldFail() throws Exception {
    createDoctor("doc_license_a", "DOC-DUP-001");

    String userJson = createDoctorJson("doc_license_b", "DOC-DUP-001");

    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.message").value("Doctor license number already exists"));
  }

  private void createDoctor(String username, String licenseNumber) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDoctorJson(username, licenseNumber)))
        .andExpect(status().isOk());
  }

  private String createDoctorJson(String username, String licenseNumber) {
    return String.format(
        "{\"username\":\"%s\",\"password\":\"password123\",\"fullName\":\"License Doctor\",\"gender\":\"MALE\",\"dob\":\"1980-01-01\",\"doctorInfo\":{\"specialization\":\"Cardiology\",\"licenseNumber\":\"%s\"},\"memberships\":[{\"hospitalId\":\"%s\",\"roleAssignments\":[{\"roleCode\":\"DOCTOR\"}]}]}",
        username, licenseNumber, hospitalId);
  }
}
