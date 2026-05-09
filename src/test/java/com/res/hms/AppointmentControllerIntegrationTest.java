package com.res.hms;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.core.domain.Gender;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppointmentControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private UUID hospitalId;
  private UUID doctorId;
  private UUID patientId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("APP_TEST", "App Hospital");
    patientId = testDataHelper.createTestPatient(hospitalId).getId();

    // Create a Doctor to book against
    UserAccount drUser = new UserAccount();
    drUser.setUsername("app_dr");
    drUser.setFullName("Dr. App");
    drUser.setPasswordHash(passwordEncoder.encode("password"));
    drUser.setActive(true);
    drUser.setGender(Gender.MALE);
    drUser.setDob(LocalDate.of(1980, 1, 1));

    DoctorProfile drProfile = new DoctorProfile();
    drProfile.setUser(drUser);
    drProfile.setSpecialization("General");
    drUser.setDoctorProfile(drProfile);

    UserRoleAssignment drAssign = new UserRoleAssignment();
    drAssign.setUser(drUser);
    drAssign.setHospitalId(hospitalId);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());
    drUser.getAssignments().add(drAssign);

    userAccountRepository.save(drUser);
    doctorId = drUser.getDoctorProfile().getId();

    // Login as the doctor (who has permission to create appointments)
    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"app_dr\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testCreateAndGetAppointmentWithVitals() throws Exception {
    VitalsDTO vitals =
        VitalsDTO.builder()
            .bloodPressure("120/80")
            .heartRate(72)
            .temperature(new BigDecimal("36.6"))
            .weight(new BigDecimal("70.5"))
            .build();

    AppointmentDTO request =
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(LocalDateTime.now().plusDays(1))
            .status(AppointmentStatus.SCHEDULED)
            .notes("Full flow test")
            .vitals(vitals)
            .build();

    String content =
        mockMvc
            .perform(
                post("/api/v1/appointments")
                    .session(session)
                    .header("X-Hospital-Id", hospitalId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.vitals.bloodPressure").value("120/80"))
            .andExpect(jsonPath("$.data.vitals.heartRate").value(72))
            .andExpect(jsonPath("$.data.vitals.temperature").value(is(36.6)))
            .andExpect(jsonPath("$.data.vitals.weight").value(is(70.5)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID appointmentId =
        UUID.fromString(objectMapper.readTree(content).path("data").path("id").asText());

    // Verify GET
    mockMvc
        .perform(
            get("/api/v1/appointments/" + appointmentId)
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.vitals.bloodPressure").value("120/80"));

    // Verify List
    mockMvc
        .perform(
            get("/api/v1/appointments")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].vitals.bloodPressure").value("120/80"));

    // Verify Update Status
    UpdateAppointmentStatusRequest statusRequest =
        new UpdateAppointmentStatusRequest(AppointmentStatus.COMPLETED);
    mockMvc
        .perform(
            post("/api/v1/appointments/" + appointmentId + "/status")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.vitals.bloodPressure").value("120/80"));

    // Verify Search
    mockMvc
        .perform(
            get("/api/v1/appointments")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("search", "Test Patient"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1));

    // Verify Update (Aggregate Update)
    request.setId(appointmentId);
    request.setNotes("Updated notes");
    request.getVitals().setBloodPressure("130/90");

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/appointments/" + appointmentId)
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.notes").value("Updated notes"))
        .andExpect(jsonPath("$.data.vitals.bloodPressure").value("130/90"));
  }

  @Test
  void testInvalidVitals() throws Exception {
    VitalsDTO invalid = VitalsDTO.builder().bloodPressure("bad").build();

    AppointmentDTO request =
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(LocalDateTime.now().plusDays(1))
            .vitals(invalid)
            .build();

    mockMvc
        .perform(
            post("/api/v1/appointments")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
