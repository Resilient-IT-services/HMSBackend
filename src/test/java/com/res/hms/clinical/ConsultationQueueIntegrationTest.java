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
class ConsultationQueueIntegrationTest {

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
        hospitalId = testDataHelper.createHospitalIfMissing("QUEUE_TEST", "Queue Hospital");

        // Create Doctor
        UserAccount drUser = new UserAccount();
        drUser.setUsername("queue_dr");
        drUser.setFullName("Dr. Queue");
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
        session = (MockHttpSession) mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"queue_dr\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession();
    }

    @Test
    void testDoctorSeesAllAppointmentsRegardlessOfVitals() throws Exception {
        com.res.hms.clinical.domain.Patient patient1 = testDataHelper.createTestPatient(hospitalId);
        com.res.hms.clinical.domain.Patient patient2 = testDataHelper.createTestPatient(hospitalId);

        // Appointment 1: SCHEDULED (No vitals)
        Appointment apt1 = new Appointment();
        apt1.setHospitalId(hospitalId);
        apt1.setDoctorId(doctorId);
        apt1.setDoctorName("Dr. Queue");
        apt1.setPatientId(patient1.getId());
        apt1.setPatientName(patient1.getFullName());
        apt1.setAppointmentTime(Instant.now());
        apt1.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(apt1);

        // Appointment 2: VITALS_RECORDED
        Appointment apt2 = new Appointment();
        apt2.setHospitalId(hospitalId);
        apt2.setDoctorId(doctorId);
        apt2.setDoctorName("Dr. Queue");
        apt2.setPatientId(patient2.getId());
        apt2.setPatientName(patient2.getFullName());
        apt2.setAppointmentTime(Instant.now().plusSeconds(3600));
        apt2.setStatus(AppointmentStatus.VITALS_RECORDED);
        appointmentRepository.save(apt2);

        // Doctor fetches their appointments
        // Without status filter: Both are visible
        mockMvc.perform(get("/api/v1/appointments")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("doctorId", doctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void testDoctorCanFilterByVitalsRecorded() throws Exception {
        com.res.hms.clinical.domain.Patient patient1 = testDataHelper.createTestPatient(hospitalId);
        com.res.hms.clinical.domain.Patient patient2 = testDataHelper.createTestPatient(hospitalId);

        // Appointment 1: SCHEDULED (No vitals)
        Appointment apt1 = new Appointment();
        apt1.setHospitalId(hospitalId);
        apt1.setDoctorId(doctorId);
        apt1.setDoctorName("Dr. Queue");
        apt1.setPatientId(patient1.getId());
        apt1.setPatientName(patient1.getFullName());
        apt1.setAppointmentTime(Instant.now());
        apt1.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(apt1);

        // Appointment 2: VITALS_RECORDED
        Appointment apt2 = new Appointment();
        apt2.setHospitalId(hospitalId);
        apt2.setDoctorId(doctorId);
        apt2.setDoctorName("Dr. Queue");
        apt2.setPatientId(patient2.getId());
        apt2.setPatientName(patient2.getFullName());
        apt2.setAppointmentTime(Instant.now().plusSeconds(3600));
        apt2.setStatus(AppointmentStatus.VITALS_RECORDED);
        appointmentRepository.save(apt2);

        // Doctor fetches their queue (only VITALS_RECORDED)
        mockMvc.perform(get("/api/v1/appointments")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .param("doctorId", doctorId.toString())
                .param("status", "VITALS_RECORDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].patientName").value(patient2.getFullName()))
                .andExpect(jsonPath("$.data.content[0].status").value("VITALS_RECORDED"));
    }
}
