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
public class UpcomingQueueIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  @Autowired
  private com.res.hms.iam.infrastructure.repository.UserAccountRepository userAccountRepository;

  @Autowired private com.res.hms.iam.infrastructure.repository.RoleRepository roleRepository;

  private UUID hospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("UPCOMING_TEST", "Upcoming Test Hospital");

    String adminUser = "upcoming_admin_" + UUID.randomUUID().toString().substring(0, 8);
    testDataHelper.createUserIfMissing(
        adminUser,
        RoleCode.ADMIN,
        hospitalId,
        "password",
        passwordEncoder,
        userAccountRepository,
        roleRepository);

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            String.format(
                                "{\"username\": \"%s\", \"password\": \"password\"}", adminUser)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testUpcomingCountWithDateFilters() throws Exception {
    Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
    Instant endOfToday = startOfToday.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.MILLIS);

    // 1. Create an appointment for tomorrow (future)
    createAppointment(
        "Tomorrow Patient",
        AppointmentStatus.SCHEDULED,
        startOfToday.plus(1, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS));

    // 2. Create an appointment for today (future time - near end of day to be safe)
    createAppointment(
        "Today Future Patient", AppointmentStatus.SCHEDULED, endOfToday.minus(1, ChronoUnit.HOURS));

    // 3. Create an appointment for today (past time - near start of day)
    createAppointment(
        "Today Past Patient", AppointmentStatus.SCHEDULED, startOfToday.plus(1, ChronoUnit.HOURS));

    // 4. Create a completed appointment for tomorrow
    createAppointment(
        "Tomorrow Completed",
        AppointmentStatus.COMPLETED,
        startOfToday.plus(1, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS));

    mockMvc
        .perform(
            get("/api/v1/appointments/stats")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("from", startOfToday.toString())
                .param("to", endOfToday.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.upcoming").value(1));
  }

  @Test
  void testUpcomingListFilter() throws Exception {
    Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
    Instant endOfToday = startOfToday.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.MILLIS);

    // 1. Create an appointment for tomorrow
    createAppointment(
        "Tomorrow Patient",
        AppointmentStatus.SCHEDULED,
        startOfToday.plus(1, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS));

    // 2. Create an appointment for today (future time)
    createAppointment(
        "Today Future Patient", AppointmentStatus.SCHEDULED, endOfToday.minus(1, ChronoUnit.HOURS));

    // 3. Create an appointment for today (past time)
    createAppointment(
        "Today Past Patient", AppointmentStatus.SCHEDULED, startOfToday.plus(1, ChronoUnit.HOURS));

    mockMvc
        .perform(
            get("/api/v1/appointments/summary")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("upcoming", "true")
                .param("from", startOfToday.toString())
                .param("to", endOfToday.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[?(@.patientName == 'Today Future Patient')]").exists())
        .andExpect(
            jsonPath("$.data.content[?(@.patientName == 'Tomorrow Patient')]").doesNotExist())
        .andExpect(
            jsonPath("$.data.content[?(@.patientName == 'Today Past Patient')]").doesNotExist());
  }

  private void createAppointment(String patientName, AppointmentStatus status, Instant time) {
    UUID doctorId =
        testDataHelper.createTestDoctor(
            hospitalId, "dr_" + UUID.randomUUID().toString().substring(0, 8));
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

    Appointment apt = new Appointment();
    apt.setHospitalId(hospitalId);
    apt.setPatientId(patientId);
    apt.setDoctorId(doctorId);
    apt.setPatientName(patientName);
    apt.setStatus(status);
    apt.setAppointmentTime(time);
    apt.setTokenNumber(101);
    appointmentRepository.save(apt);
  }
}
