package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.SpecializationRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
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
class ActiveStatusIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private SpecializationRepository specializationRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("ACTIVE_TEST", "Active Hospital");

    UserAccount admin = new UserAccount();
    admin.setUsername("admin_active_test");
    admin.setPasswordHash(passwordEncoder.encode("admin123"));
    admin.setSuperuser(true);
    admin.setActive(true);
    userAccountRepository.saveAndFlush(admin);

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"username\": \"admin_active_test\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void inactiveDoctorsShouldNotBeListed() throws Exception {
    // 1. Create an inactive doctor
    String username = "inactive_dr";
    testDataHelper.createUserIfMissing(
        username,
        RoleCode.DOCTOR,
        testHospitalId,
        "password",
        passwordEncoder,
        userAccountRepository,
        roleRepository);

    UserAccount doctor = userAccountRepository.findByUsername(username).orElseThrow();
    doctor.setActive(false);
    userAccountRepository.saveAndFlush(doctor);

    // 2. Check /api/v1/doctors (DoctorController)
    mockMvc
        .perform(
            get("/api/v1/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[?(@.username=='" + username + "')]").doesNotExist());

    // 3. Check /api/v1/staff/doctors (StaffController)
    mockMvc
        .perform(
            get("/api/v1/staff/doctors")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.content[?(@.fullName=='" + doctor.getFullName() + "')]")
                .doesNotExist());
  }

  @Test
  void cannotBookAppointmentWithInactiveDoctor() throws Exception {
    // 1. Create an inactive doctor
    String username = "inactive_dr_booking";
    testDataHelper.createUserIfMissing(
        username,
        RoleCode.DOCTOR,
        testHospitalId,
        "password",
        passwordEncoder,
        userAccountRepository,
        roleRepository);

    UserAccount doctor = userAccountRepository.findByUsername(username).orElseThrow();
    doctor.setActive(false);

    // Ensure DoctorProfile exists (it should be created if needed, but let's check)
    if (doctor.getDoctorProfile() == null) {
      com.res.hms.iam.domain.DoctorProfile profile = new com.res.hms.iam.domain.DoctorProfile();
      profile.setUser(doctor);
      com.res.hms.iam.domain.Specialization spec = new com.res.hms.iam.domain.Specialization();
      spec.setName("General");
      specializationRepository.save(spec);
      profile.getSpecializations().add(spec);
      doctor.setDoctorProfile(profile);
    }

    userAccountRepository.saveAndFlush(doctor);

    com.res.hms.clinical.domain.Patient patient = testDataHelper.createTestPatient(testHospitalId);

    // 2. Try to book appointment
    String appointmentJson =
        String.format(
            "{\"patientId\":\"%s\", \"doctorId\":\"%s\", \"appointmentTime\":\"2026-06-20T10:00:00Z\", \"status\":\"SCHEDULED\"}",
            patient.getId(), doctor.getDoctorProfile().getId());

    mockMvc
        .perform(
            post("/api/v1/appointments")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentJson))
        .andExpect(status().isNotFound());
  }
}
