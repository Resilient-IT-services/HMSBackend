package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
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
class ReceptionistRestrictionIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession receptionistSession;
  private UUID appointmentId;

  @BeforeEach
  void setUp() throws Exception {
    testHospitalId =
        testDataHelper.createHospitalIfMissing("RESTRICT_TEST", "Restrict Test Hospital");

    // Create Receptionist
    UserAccount receptionist = new UserAccount();
    receptionist.setUsername("test_receptionist");
    receptionist.setPasswordHash(passwordEncoder.encode("password"));
    receptionist.setActive(true);
    UserContract recepAssign = new UserContract();
    recepAssign.setUser(receptionist);
    recepAssign.setHospitalId(testHospitalId);
    recepAssign.setRole(roleRepository.findByCode(RoleCode.RECEPTIONIST).orElseThrow());
    receptionist.getContracts().add(recepAssign);
    userAccountRepository.save(receptionist);

    receptionistSession =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"username\": \"test_receptionist\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

    // Create valid Patient and Doctor
    com.res.hms.clinical.domain.Patient patient = testDataHelper.createTestPatient(testHospitalId);

    // Create a valid Visit
    com.res.hms.clinical.domain.PatientVisit visit =
        testDataHelper.createTestVisit(testHospitalId, patient.getId());

    UserAccount doctor = new UserAccount();
    doctor.setUsername("test_doctor_restriction");
    doctor.setPasswordHash(passwordEncoder.encode("password"));
    doctor.setActive(true);
    com.res.hms.iam.domain.DoctorProfile drProfile = new com.res.hms.iam.domain.DoctorProfile();
    drProfile.setUser(doctor);
    doctor.setDoctorProfile(drProfile);
    UserContract drAssign = new UserContract();
    drAssign.setUser(doctor);
    drAssign.setHospitalId(testHospitalId);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());
    doctor.getContracts().add(drAssign);
    userAccountRepository.saveAndFlush(doctor);

    // Create an Appointment in CONSULTATION_STARTED status
    Appointment appointment = new Appointment();
    appointment.setHospitalId(testHospitalId);
    appointment.setPatientId(patient.getId());
    appointment.setPatientName(patient.getFullName());
    appointment.setDoctorId(doctor.getId()); // Use doctor.getId() directly since MapsId is used
    appointment.setDoctorName(doctor.getFullName());
    appointment.setAppointmentTime(
        java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
    appointment.setStatus(AppointmentStatus.CONSULTATION_STARTED);
    appointment.setTokenNumber(1);
    appointment.setVisitId(visit.getId());
    appointment = appointmentRepository.saveAndFlush(appointment);
    appointmentId = appointment.getId();
  }

  @Test
  void testReceptionistCannotUpdateStatusAfterConsultationStarted() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/appointments/" + appointmentId + "/status")
                .session(receptionistSession)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"COMPLETED\"}"))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest());
  }

  @Test
  void testReceptionistCannotUpdateAppointmentAfterConsultationStarted() throws Exception {
    Appointment appt = appointmentRepository.findById(appointmentId).get();

    String content =
        String.format(
            "{\"id\": \"%s\", \"patientId\": \"%s\", \"doctorId\": \"%s\", \"appointmentTime\": \"%s\", \"notes\": \"updated notes\"}",
            appointmentId, appt.getPatientId(), appt.getDoctorId(), appt.getAppointmentTime());

    mockMvc
        .perform(
            put("/api/v1/appointments/" + appointmentId)
                .session(receptionistSession)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
        .andExpect(status().isBadRequest());
  }
}
