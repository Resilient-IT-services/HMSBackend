package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.domain.AppointmentStatus;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.DoctorSchedule;
import com.res.hms.domain.Gender;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
class DoctorControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private com.res.hms.appointment.repository.AppointmentRepository appointmentRepository;

  @Autowired
  private com.res.hms.repository.DoctorAvailabilityExceptionRepository
      availabilityExceptionRepository;

  @Autowired private com.res.hms.repository.HospitalRepository hospitalRepository;

  private UUID hospitalId;
  private UUID doctorId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("AVAIL_TEST", "Avail Hospital");

    // Create Doctor
    UserAccount drUser = new UserAccount();
    drUser.setUsername("avail_dr");
    drUser.setFullName("Dr. Available");
    drUser.setPasswordHash(passwordEncoder.encode("password"));
    drUser.setActive(true);
    drUser.setGender(Gender.MALE);
    drUser.setDob(LocalDate.of(1980, 1, 1));

    Doctor drProfile = new Doctor();
    drProfile.setUser(drUser);
    drProfile.setSpecialization("General");
    drUser.setDoctor(drProfile);

    UserRoleAssignment drAssign = new UserRoleAssignment();
    drAssign.setUser(drUser);
    drAssign.setHospitalId(hospitalId);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());

    // Add Schedule: Monday 9-11 AM, 15 min slots
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setAssignment(drAssign);
    schedule.setDayOfWeek((short) 1); // Monday
    schedule.setStartTime(LocalTime.of(9, 0));
    schedule.setEndTime(LocalTime.of(11, 0));
    schedule.setSlotDurationMinutes(60); // 1 hour slots for easy test
    drAssign.getSchedules().add(schedule);

    drUser.getAssignments().add(drAssign);
    UserAccount saved = userAccountRepository.save(drUser);
    doctorId = saved.getDoctor().getId();

    // Session
    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"avail_dr\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testGetAvailability() throws Exception {
    LocalDate nextMonday = LocalDate.of(2026, 5, 4); // A future Monday

    // Initial check: Should have 1 session (9-11 AM)
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", nextMonday.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions.length()").value(1))
        .andExpect(jsonPath("$.data.sessions[0].startTime").value("09:00"))
        .andExpect(jsonPath("$.data.sessions[0].currentBookings").value(0))
        .andExpect(jsonPath("$.data.sessions[0].nextSuggestedTime").value("09:00"));

    // Book 1 patient
    com.res.hms.domain.Appointment apt = new com.res.hms.domain.Appointment();
    apt.setHospitalId(hospitalId);
    apt.setDoctor(userAccountRepository.findByUsername("avail_dr").get().getDoctor());
    apt.setPatient(testDataHelper.createTestPatient(hospitalId));
    apt.setAppointmentTime(LocalDateTime.of(nextMonday, LocalTime.of(9, 0)));
    apt.setStatus(AppointmentStatus.SCHEDULED);
    appointmentRepository.save(apt);

    // Check again: currentBookings should be 1, suggestedTime should be 10:00 (since duration was
    // 60 min in setup)
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", nextMonday.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions[0].currentBookings").value(1))
        .andExpect(jsonPath("$.data.sessions[0].nextSuggestedTime").value("10:00"));
  }

  @Test
  void testGetAvailabilityWithModifiedException() throws Exception {
    LocalDate tuesday = LocalDate.of(2026, 4, 28); // Tuesday

    // 1. Create a MODIFIED exception for Tuesday: 2-4 PM
    com.res.hms.domain.DoctorAvailabilityException ex =
        new com.res.hms.domain.DoctorAvailabilityException();
    ex.setDoctor(userAccountRepository.findByUsername("avail_dr").get().getDoctor());
    ex.setHospital(hospitalRepository.findById(hospitalId).orElseThrow());
    ex.setDate(tuesday);
    ex.setType("MODIFIED");
    ex.setStartTime(LocalTime.of(14, 0));
    ex.setEndTime(LocalTime.of(16, 0));
    ex.setSlotDurationMinutes(30);
    ex.setNote("Afternoon shift");
    availabilityExceptionRepository.save(ex);

    // 2. Fetch availability for Tuesday
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", tuesday.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions.length()").value(1))
        .andExpect(jsonPath("$.data.sessions[0].startTime").value("14:00"))
        .andExpect(jsonPath("$.data.sessions[0].endTime").value("16:00"))
        .andExpect(jsonPath("$.data.sessions[0].nextSuggestedTime").value("14:00"));
  }
}
