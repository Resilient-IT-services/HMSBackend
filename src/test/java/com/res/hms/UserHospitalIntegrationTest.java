package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserHospitalIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

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
  void testCreateHospitalAndUser() throws Exception {
    // 1. Create Hospital
    String hospitalJson =
        "{\"name\":\"New Hospital\",\"code\":\"NEW_HOSP\",\"address\":\"New City\",\"status\":\"ACTIVE\"}";

    String hospitalResponse =
        mockMvc
            .perform(
                post("/api/v1/hospitals")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(hospitalJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("New Hospital"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID hospitalId =
        UUID.fromString(objectMapper.readTree(hospitalResponse).path("data").path("id").asText());

    // 2. Create User for this hospital
    String userJson =
        String.format(
            "{\"username\":\"new_staff\",\"password\":\"password123\",\"fullName\":\"New Staff\",\"gender\":\"MALE\",\"dob\":\"1995-01-01\",\"doctorInfo\":{\"specialization\":\"Heart\",\"licenseNumber\":\"NEW-STAFF-DOC-001\"},\"memberships\":[{\"hospitalId\":\"%s\",\"roleAssignments\":[{\"roleCode\":\"DOCTOR\"}]}]}",
            hospitalId);

    String userResponse =
        mockMvc
            .perform(
                post("/api/v1/users")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(userJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("new_staff"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID userId =
        UUID.fromString(objectMapper.readTree(userResponse).path("data").path("id").asText());

    // 3. Update User Profile (triggers UserRoleUpdateService)
    String updateJson =
        String.format(
            "{\"username\":\"new_staff\",\"fullName\":\"Updated Staff\",\"gender\":\"MALE\",\"dob\":\"1995-01-01\",\"memberships\":[{\"hospitalId\":\"%s\",\"roleAssignments\":[{\"roleCode\":\"NURSE\",\"nurseDetails\":{\"department\":\"ICU\"}},{\"roleCode\":\"PHARMACIST\",\"pharmacistDetails\":{\"licenseNumber\":\"PH-123\"}}]}]}",
            hospitalId);

    mockMvc
        .perform(
            put("/api/v1/users/" + userId)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fullName").value("Updated Staff"));

    // 4. List Users
    mockMvc
        .perform(get("/api/v1/users").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThan(1)));
  }

  @Test
  void testGlobalExceptionHandler() throws Exception {
    // Trigger 404
    mockMvc
        .perform(get("/api/v1/hospitals/" + UUID.randomUUID()).session(session))
        .andExpect(status().isNotFound());

    // Trigger 400
    mockMvc
        .perform(
            post("/api/v1/hospitals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }
}
