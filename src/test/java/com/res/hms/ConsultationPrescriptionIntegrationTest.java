package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionItemDTO;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
class ConsultationPrescriptionIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  @Autowired
  private com.res.hms.clinical.infrastructure.repository.AppointmentRepository
      appointmentRepository;

  @Autowired private MedicineRepository medicineRepository;
  @Autowired private ObjectMapper objectMapper;

  private UUID hospitalId;
  private UUID appointmentId;
  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("CONS_PRES_TEST", "Consultation Hospital");

    // Create Doctor
    UserAccount drUser = new UserAccount();
    drUser.setUsername("cons_dr");
    drUser.setFullName("Dr. Consultant");
    drUser.setPasswordHash(passwordEncoder.encode("password"));
    drUser.setActive(true);
    drUser.setGender(com.res.hms.core.domain.Gender.MALE);
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
    com.res.hms.clinical.domain.Patient patient = testDataHelper.createTestPatient(hospitalId);

    // Create Appointment
    Appointment apt = new Appointment();
    apt.setHospitalId(hospitalId);
    apt.setDoctorId(saved.getDoctorProfile().getId());
    apt.setDoctorName(saved.getFullName());
    apt.setPatientId(patient.getId());
    apt.setPatientName(patient.getFullName());
    apt.setAppointmentTime(Instant.now().plus(1, ChronoUnit.DAYS));
    apt.setStatus(AppointmentStatus.VITALS_RECORDED); // Start from VITALS_RECORDED
    appointmentId = appointmentRepository.save(apt).getId();

    // Session
    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"cons_dr\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
  }

  @Test
  void testUpdateConsultationSavesPrescription() throws Exception {
    // 1. Create Medicine
    Medicine med = new Medicine();
    med.setName("Amoxicillin");
    UUID medicineId = medicineRepository.save(med).getId();

    // 2. Prepare AppointmentDTO with Prescription
    PrescriptionDTO prescriptionDto =
        PrescriptionDTO.builder()
            .items(
                List.of(
                    PrescriptionItemDTO.builder()
                        .medicineId(medicineId)
                        .dosage("250mg")
                        .duration("5 days")
                        .build()))
            .build();

    AppointmentDTO appointmentDto =
        AppointmentDTO.builder()
            .id(appointmentId)
            .patientId(UUID.randomUUID()) // Not used by updateConsultation
            .doctorId(UUID.randomUUID()) // Not used by updateConsultation
            .appointmentTime(OffsetDateTime.now(ZoneOffset.UTC)) // Not used by updateConsultation
            .chiefComplaints("Fever")
            .diagnosis("Bacterial Infection")
            .prescription(prescriptionDto)
            .build();

    // 3. Perform updateConsultation
    mockMvc
        .perform(
            patch("/api/v1/appointments/" + appointmentId + "/consultation")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONSULTATION_STARTED"))
        .andExpect(jsonPath("$.data.prescription.items.length()").value(1))
        .andExpect(jsonPath("$.data.prescription.items[0].medicineName").value("Amoxicillin"))
        .andExpect(jsonPath("$.data.prescription.items[0].dosage").value("250mg"));
  }

  @Test
  void testUpdateConsultationSavesFreeTextMedicine() throws Exception {
    // 1. Prepare AppointmentDTO with Free-text Prescription (null medicineId)
    PrescriptionDTO prescriptionDto =
        PrescriptionDTO.builder()
            .items(
                List.of(
                    PrescriptionItemDTO.builder()
                        .medicineId(null) // Free-text
                        .medicineName("Custom Herb")
                        .dosage("10ml")
                        .duration("1 week")
                        .build()))
            .build();

    AppointmentDTO appointmentDto =
        AppointmentDTO.builder()
            .id(appointmentId)
            .patientId(UUID.randomUUID())
            .doctorId(UUID.randomUUID())
            .appointmentTime(OffsetDateTime.now(ZoneOffset.UTC))
            .chiefComplaints("Natural healing request")
            .prescription(prescriptionDto)
            .build();

    // 2. Perform updateConsultation
    mockMvc
        .perform(
            patch("/api/v1/appointments/" + appointmentId + "/consultation")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.prescription.items.length()").value(1))
        .andExpect(jsonPath("$.data.prescription.items[0].medicineId").doesNotExist())
        .andExpect(jsonPath("$.data.prescription.items[0].medicineName").value("Custom Herb"))
        .andExpect(jsonPath("$.data.prescription.items[0].dosage").value("10ml"));
  }
}
