package com.res.hms.clinical;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.domain.Gender;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.time.Instant;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppointmentSummaryFilteringIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private AppointmentRepository appointmentRepository;

  private UUID hospitalId;
  private UUID doctorId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("FILTER_TEST", "Filter Hospital");

    // Create Admin for session (RECEPTION_ACCESS)
    UserAccount adminUser = new UserAccount();
    adminUser.setUsername("filter_admin");
    adminUser.setFullName("Filter Admin");
    adminUser.setPasswordHash(passwordEncoder.encode("password"));
    adminUser.setActive(true);
    adminUser.setGender(Gender.MALE);
    adminUser.setDob(LocalDate.of(1980, 1, 1));

    UserContract adminContract = new UserContract();
    adminContract.setUser(adminUser);
    adminContract.setHospitalId(hospitalId);
    adminContract.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());

    adminUser.getContracts().add(adminContract);
    userAccountRepository.save(adminUser);

    // Create Doctor
    UserAccount drUser = new UserAccount();
    drUser.setUsername("filter_dr");
    drUser.setFullName("Dr. Gregory House");
    drUser.setPasswordHash(passwordEncoder.encode("password"));
    drUser.setActive(true);
    drUser.setGender(Gender.MALE);
    drUser.setDob(LocalDate.of(1980, 1, 1));

    DoctorProfile drProfile = new DoctorProfile();
    drProfile.setUser(drUser);
    drUser.setDoctorProfile(drProfile);

    UserContract drAssign = new UserContract();
    drAssign.setUser(drUser);
    drAssign.setHospitalId(hospitalId);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());

    drUser.getContracts().add(drAssign);
    UserAccount saved = userAccountRepository.save(drUser);
    doctorId = saved.getDoctorProfile().getId();

    // Session
    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"filter_admin\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testListSummaryWithNewFilters() throws Exception {
    com.res.hms.clinical.domain.Patient patient1 = testDataHelper.createTestPatient(hospitalId);
    patient1.setFullName("John Doe");

    Appointment apt1 = new Appointment();
    apt1.setHospitalId(hospitalId);
    apt1.setDoctorId(doctorId);
    apt1.setDoctorName("Dr. Gregory House");
    apt1.setPatientId(patient1.getId());
    apt1.setPatientName(patient1.getFullName());
    apt1.setAppointmentTime(Instant.now());
    apt1.setStatus(AppointmentStatus.SCHEDULED);
    apt1.setTokenNumber(123);
    appointmentRepository.save(apt1);

    com.res.hms.clinical.domain.Patient patient2 = testDataHelper.createTestPatient(hospitalId);
    patient2.setFullName("Jane Smith");

    Appointment apt2 = new Appointment();
    apt2.setHospitalId(hospitalId);
    apt2.setDoctorId(doctorId);
    apt2.setDoctorName("Dr. Gregory House");
    apt2.setPatientId(patient2.getId());
    apt2.setPatientName(patient2.getFullName());
    apt2.setAppointmentTime(Instant.now().plusSeconds(3600));
    apt2.setStatus(AppointmentStatus.COMPLETED);
    apt2.setTokenNumber(456);
    appointmentRepository.save(apt2);

    // Filter by token
    mockMvc
        .perform(
            get("/api/v1/appointments/summary")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("token", "123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].tokenNumber").value(123));

    // Filter by patientName
    mockMvc
        .perform(
            get("/api/v1/appointments/summary")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("patientName", "Jane"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].patientName").value("Jane Smith"));

    // Filter by doctor (partial name)
    mockMvc
        .perform(
            get("/api/v1/appointments/summary")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("doctor", "House"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2));

    // Filter by status
    mockMvc
        .perform(
            get("/api/v1/appointments/summary")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("status", "COMPLETED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"));

    // Test Sort
    mockMvc
        .perform(
            get("/api/v1/appointments/summary")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("sort", "tokenNumber,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].tokenNumber").value(456))
        .andExpect(jsonPath("$.data.content[1].tokenNumber").value(123));
  }
}
