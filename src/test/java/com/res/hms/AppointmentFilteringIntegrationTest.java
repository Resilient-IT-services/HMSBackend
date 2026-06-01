package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.iam.domain.RoleCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class AppointmentFilteringIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  @Autowired private com.res.hms.iam.infrastructure.repository.UserAccountRepository userAccountRepository;
  @Autowired private com.res.hms.iam.infrastructure.repository.RoleRepository roleRepository;

  private UUID hospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("APT_TEST", "Appointment Test Hospital");
    
    String adminUser = "apt_admin_" + UUID.randomUUID().toString().substring(0, 8);
    testDataHelper.createUserIfMissing(adminUser, RoleCode.ADMIN, hospitalId, "password", passwordEncoder, userAccountRepository, roleRepository);

    session = (MockHttpSession) mockMvc
        .perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format("{\"username\": \"%s\", \"password\": \"password\"}", adminUser)))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession();
  }

  @Test
  void testAppointmentListingWithFilteringAndSorting() throws Exception {
    UUID doctorId = testDataHelper.createTestDoctor(hospitalId, "dr_apt_test");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

    // 1. Create multiple appointments
    createAppointment("Patient A", "Dr. X", AppointmentStatus.SCHEDULED, Instant.now().plus(1, ChronoUnit.DAYS));
    createAppointment("Patient B", "Dr. Y", AppointmentStatus.COMPLETED, Instant.now().minus(1, ChronoUnit.DAYS));
    createAppointment("Patient C", "Dr. X", AppointmentStatus.CANCELLED, Instant.now().plus(2, ChronoUnit.DAYS));

    // 2. Filter by status=SCHEDULED
    mockMvc.perform(get("/api/v1/appointments")
            .session(session)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("status", "SCHEDULED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].patientName").value("Patient A"));

    // 3. Search by doctorName
    mockMvc.perform(get("/api/v1/appointments")
            .session(session)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("search", "Dr. Y"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].patientName").value("Patient B"));

    // 4. Sort by patientName desc
    mockMvc.perform(get("/api/v1/appointments")
            .session(session)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("sort", "patientName,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].patientName").value("Patient C"))
        .andExpect(jsonPath("$.data.content[1].patientName").value("Patient B"))
        .andExpect(jsonPath("$.data.content[2].patientName").value("Patient A"));
        
    // 5. Date range filter
    mockMvc.perform(get("/api/v1/appointments")
            .session(session)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("from", Instant.now().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2)); // A and C are in the future
  }

  private void createAppointment(String patientName, String doctorName, AppointmentStatus status, Instant time) {
    UUID doctorId = testDataHelper.createTestDoctor(hospitalId, "dr_" + UUID.randomUUID().toString().substring(0, 8));
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

    Appointment apt = new Appointment();
    apt.setHospitalId(hospitalId);
    apt.setPatientId(patientId);
    apt.setDoctorId(doctorId);
    apt.setPatientName(patientName);
    apt.setDoctorName(doctorName);
    apt.setStatus(status);
    apt.setAppointmentTime(time);
    apt.setTokenNumber(101);
    appointmentRepository.save(apt);
  }
}
