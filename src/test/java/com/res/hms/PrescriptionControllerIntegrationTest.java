package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.domain.AppointmentStatus;
import com.res.hms.domain.Gender;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.prescription.dto.PrescriptionDTO;
import com.res.hms.prescription.dto.PrescriptionItemDTO;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
class PrescriptionControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private com.res.hms.appointment.repository.AppointmentRepository appointmentRepository;
  @Autowired private com.res.hms.repository.MedicineRepository medicineRepository;
  @Autowired private ObjectMapper objectMapper;

  private UUID hospitalId;
  private UUID doctorId;
  private UUID appointmentId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("PRES_TEST", "Prescription Hospital");

    // Create Doctor
    UserAccount drUser = new UserAccount();
    drUser.setUsername("pres_dr");
    drUser.setFullName("Dr. Prescriber");
    drUser.setPasswordHash(passwordEncoder.encode("password"));
    drUser.setActive(true);
    drUser.setGender(Gender.MALE);
    drUser.setDob(LocalDate.of(1980, 1, 1));

    com.res.hms.domain.DoctorProfile drProfile = new com.res.hms.domain.DoctorProfile();
    drProfile.setUser(drUser);
    drProfile.setSpecialization("General");
    drUser.setDoctorProfile(drProfile);

    com.res.hms.domain.UserRoleAssignment drAssign = new com.res.hms.domain.UserRoleAssignment();
    drAssign.setUser(drUser);
    drAssign.setHospitalId(hospitalId);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());

    drUser.getAssignments().add(drAssign);
    UserAccount saved = userAccountRepository.save(drUser);
    doctorId = saved.getDoctorProfile().getId();

    // Create Appointment
    com.res.hms.domain.Appointment apt = new com.res.hms.domain.Appointment();
    apt.setHospitalId(hospitalId);
    apt.setDoctorProfile(saved.getDoctorProfile());
    apt.setPatient(testDataHelper.createTestPatient(hospitalId));
    apt.setAppointmentTime(LocalDateTime.now().plusDays(1));
    apt.setStatus(AppointmentStatus.SCHEDULED);
    appointmentId = appointmentRepository.save(apt).getId();

    // Session
    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"pres_dr\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testCreateAndGetPrescription() throws Exception {
    // 1. Create Medicine
    com.res.hms.domain.Medicine med = new com.res.hms.domain.Medicine();
    med.setHospitalId(hospitalId);
    med.setName("Paracetamol");
    UUID medicineId = medicineRepository.save(med).getId();

    // 2. Create Prescription
    PrescriptionDTO dto =
        PrescriptionDTO.builder()
            .appointmentId(appointmentId)
            .items(
                List.of(
                    PrescriptionItemDTO.builder()
                        .medicineId(medicineId)
                        .dosage("500mg")
                        .duration("3 days")
                        .build()))
            .build();

    mockMvc
        .perform(
            post("/api/v1/pharmacy/prescriptions")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appointmentId").value(appointmentId.toString()))
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].medicineName").value("Paracetamol"));

    // 3. Get Prescription by ID (which is same as appointmentId)
    mockMvc
        .perform(
            get("/api/v1/pharmacy/prescriptions/" + appointmentId)
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(appointmentId.toString()))
        .andExpect(jsonPath("$.data.items[0].dosage").value("500mg"));
  }

  @Test
  void testCreateDuplicatePrescriptionFails() throws Exception {
    // 1. Create first prescription
    PrescriptionDTO dto =
        PrescriptionDTO.builder().appointmentId(appointmentId).items(List.of()).build();

    mockMvc
        .perform(
            post("/api/v1/pharmacy/prescriptions")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk());

    // 2. Try to create second prescription for same appointment
    mockMvc
        .perform(
            post("/api/v1/pharmacy/prescriptions")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict());
  }
}
