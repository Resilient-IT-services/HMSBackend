package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.domain.DoctorAvailabilityOverride;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.clinical.domain.ExceptionType;
import com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityOverrideRepository;
import com.res.hms.core.domain.Gender;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.time.DayOfWeek;
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

  @Autowired
  private com.res.hms.clinical.infrastructure.repository.AppointmentRepository
      appointmentRepository;

  @Autowired
  private com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository
      scheduleRepository;

  @Autowired private DoctorAvailabilityOverrideRepository availabilityOverrideRepository;

  @Autowired private HospitalRepository hospitalRepository;

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

    DoctorProfile drProfile = new DoctorProfile();
    drProfile.setUser(drUser);
    com.res.hms.iam.domain.Specialization spec = new com.res.hms.iam.domain.Specialization();
    spec.setName("General");
    drProfile.getSpecializations().add(spec);
    drUser.setDoctorProfile(drProfile);

    UserContract drAssign = new UserContract();
    drAssign.setUser(drUser);
    drAssign.setHospitalId(hospitalId);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());

    drUser.getContracts().add(drAssign);
    UserAccount saved = userAccountRepository.save(drUser);
    doctorId = saved.getDoctorProfile().getId();
    UUID assignmentId = saved.getContracts().iterator().next().getId();

    // Add Schedule: Monday 9-11 AM, 1 hour slots
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setAssignmentId(assignmentId);
    schedule.setDayOfWeek(DayOfWeek.MONDAY); // Monday
    schedule.setStartTime(LocalTime.of(9, 0));
    schedule.setEndTime(LocalTime.of(11, 0));
    schedule.setSlotDurationMinutes(60);
    scheduleRepository.save(schedule);

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
    LocalDate nextMonday = LocalDate.now().plusWeeks(1);
    while (nextMonday.getDayOfWeek().getValue() != 1) {
      nextMonday = nextMonday.plusDays(1);
    }

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
    UserAccount drUser = userAccountRepository.findByUsername("avail_dr").get();
    com.res.hms.clinical.domain.Patient patient = testDataHelper.createTestPatient(hospitalId);

    Appointment apt = new Appointment();
    apt.setHospitalId(hospitalId);
    apt.setDoctorId(drUser.getDoctorProfile().getId());
    apt.setDoctorName(drUser.getFullName());
    apt.setPatientId(patient.getId());
    apt.setPatientName(patient.getFullName());
    apt.setAppointmentTime(
        LocalDateTime.of(nextMonday, LocalTime.of(9, 0))
            .atZone(java.time.ZoneId.of("Asia/Kolkata"))
            .toInstant());
    apt.setStatus(AppointmentStatus.SCHEDULED);
    appointmentRepository.save(apt);

    // Check again
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
    LocalDate tuesday = LocalDate.now().plusDays(7); // Use a future date
    while (tuesday.getDayOfWeek() != DayOfWeek.TUESDAY) {
      tuesday = tuesday.plusDays(1);
    }
    UserAccount drUser = userAccountRepository.findByUsername("avail_dr").get();

    // 1. Create an ADDED override for Tuesday: 10 AM-4 PM
    DoctorAvailabilityOverride override = new DoctorAvailabilityOverride();
    override.setDoctorId(drUser.getDoctorProfile().getId());
    override.setHospitalId(hospitalId);
    override.setDate(tuesday);
    override.setType(ExceptionType.ADDED);
    override.setStartTime(LocalTime.of(10, 0));
    override.setEndTime(LocalTime.of(16, 0));
    override.setSlotDurationMinutes(30);
    override.setNote("Afternoon shift");
    availabilityOverrideRepository.save(override);

    // 2. Fetch availability for Tuesday
    mockMvc
        .perform(
            get("/api/v1/doctors/" + doctorId + "/availability")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("date", tuesday.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessions.length()").value(1))
        .andExpect(jsonPath("$.data.sessions[0].startTime").value("10:00"))
        .andExpect(jsonPath("$.data.sessions[0].endTime").value("16:00"))
        .andExpect(jsonPath("$.data.sessions[0].nextSuggestedTime").value("10:00"));
  }
}
