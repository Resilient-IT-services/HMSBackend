package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionItemDTO;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;

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
  @Autowired private com.res.hms.clinical.infrastructure.repository.AppointmentRepository appointmentRepository;
  @Autowired private MedicineRepository medicineRepository;
  @Autowired private ObjectMapper objectMapper;

  private UUID hospitalId;
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
    drUser.setGender(com.res.hms.core.domain.Gender.MALE);
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
    UserAccount saved = userAccountRepository.save(drUser);
    com.res.hms.clinical.domain.Patient patient = testDataHelper.createTestPatient(hospitalId);

    // Create Appointment
    Appointment apt = new Appointment();
    apt.setHospitalId(hospitalId);
    apt.setDoctorId(saved.getDoctorProfile().getId());
    apt.setDoctorName(saved.getFullName());
    apt.setPatientId(patient.getId());
    apt.setPatientName(patient.getFullName());
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
    Medicine med = new Medicine();
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

    var response = mockMvc
        .perform(
            post("/api/v1/pharmacy/prescriptions")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appointmentId").value(appointmentId.toString()))
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].medicineName").value("Paracetamol"))
        .andReturn();

    String content = response.getResponse().getContentAsString();
    UUID prescriptionId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(content, "$.data.id"));

    // 3. Get Prescription by its actual ID
    mockMvc
        .perform(
            get("/api/v1/pharmacy/prescriptions/" + prescriptionId)
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(prescriptionId.toString()))
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
