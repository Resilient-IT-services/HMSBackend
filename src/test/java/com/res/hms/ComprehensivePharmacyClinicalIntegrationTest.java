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
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.math.BigDecimal;
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
class ComprehensivePharmacyClinicalIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private com.res.hms.clinical.infrastructure.repository.AppointmentRepository appointmentRepository;
  @Autowired private MedicineRepository medicineRepository;
  @Autowired private PharmacyStockRepository pharmacyStockRepository;
  @Autowired private com.res.hms.iam.infrastructure.repository.PharmacistHospitalDetailsRepository pharmacistHospitalDetailsRepository;
  @Autowired private com.res.hms.clinical.application.prescription.repository.PrescriptionRepository prescriptionRepository;
  @Autowired private jakarta.persistence.EntityManager entityManager;
  @Autowired private ObjectMapper objectMapper;

  private UUID hospitalId;
  private UUID doctorProfileId;
  private MockHttpSession doctorSession;
  private MockHttpSession pharmacistSession;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("COMP_TEST", "Comprehensive Test Hospital");

    // 1. Create Doctor
    UserAccount drUser = createAccount("comp_dr", RoleCode.DOCTOR);
    doctorProfileId = drUser.getDoctorProfile().getId();
    doctorSession = login("comp_dr");

    // 2. Create Pharmacist
    createAccount("comp_ph", RoleCode.PHARMACIST);
    pharmacistSession = login("comp_ph");
  }

  private UserAccount createAccount(String username, RoleCode roleCode) {
    UserAccount user = new UserAccount();
    user.setUsername(username);
    user.setFullName(username.toUpperCase());
    user.setPasswordHash(passwordEncoder.encode("password"));
    user.setActive(true);
    user.setGender(com.res.hms.core.domain.Gender.MALE);
    user.setDob(LocalDate.of(1980, 1, 1));

    UserContract contract = new UserContract();
    contract.setUser(user);
    contract.setHospitalId(hospitalId);
    contract.setRole(roleRepository.findByCode(roleCode).orElseThrow());
    user.getContracts().add(contract);

    if (roleCode == RoleCode.DOCTOR) {
      DoctorProfile drProfile = new DoctorProfile();
      drProfile.setUser(user);
      com.res.hms.iam.domain.Specialization spec = new com.res.hms.iam.domain.Specialization();
      spec.setName("General");
      drProfile.getSpecializations().add(spec);
      user.setDoctorProfile(drProfile);
    }

    UserAccount saved = userAccountRepository.save(user);

    if (roleCode == RoleCode.PHARMACIST) {
      com.res.hms.iam.domain.PharmacistHospitalDetails phDetails =
          new com.res.hms.iam.domain.PharmacistHospitalDetails();
      phDetails.setAssignment(saved.getContracts().iterator().next());
      phDetails.setCounterLocation("Main Counter");
      pharmacistHospitalDetailsRepository.save(phDetails);
    }

    return saved;
  }

  private MockHttpSession login(String username) throws Exception {
    return (MockHttpSession) mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\": \"" + username + "\", \"password\": \"password\"}"))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession();
  }

  @Test
  void testClinicalConsultationWithMixedMedicines() throws Exception {
    // 1. Setup Patient and Appointment
    com.res.hms.clinical.domain.Patient patient = testDataHelper.createTestPatient(hospitalId);
    Appointment apt = new Appointment();
    apt.setHospitalId(hospitalId);
    apt.setDoctorId(doctorProfileId);
    apt.setPatientId(patient.getId());
    apt.setPatientName(patient.getFullName());
    apt.setAppointmentTime(Instant.now());
    apt.setStatus(AppointmentStatus.VITALS_RECORDED);
    apt.setTokenNumber(123);
    UUID appointmentId = appointmentRepository.save(apt).getId();

    // 2. Create an existing medicine in catalog
    Medicine catalogMed = new Medicine();
    catalogMed.setName("Catalog Medicine");
    UUID catalogMedId = medicineRepository.save(catalogMed).getId();

    // 3. Submit Consultation with mixed prescription (catalog + custom)
    PrescriptionDTO prescriptionDto = PrescriptionDTO.builder()
        .items(List.of(
            PrescriptionItemDTO.builder().medicineId(catalogMedId).dosage("1-0-1").duration("5 days").build(),
            PrescriptionItemDTO.builder().medicineId(null).medicineName("Custom Herbal").dosage("5ml").duration("3 days").build()
        )).build();

    AppointmentDTO updateDto = AppointmentDTO.builder()
        .id(appointmentId)
        .patientId(patient.getId())
        .doctorId(doctorProfileId)
        .appointmentTime(OffsetDateTime.now(ZoneOffset.UTC))
        .chiefComplaints("Fever")
        .prescription(prescriptionDto)
        .build();

    mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/consultation")
            .session(doctorSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.prescription.items.length()").value(2))
        .andExpect(jsonPath("$.data.prescription.items[0].medicineName").value("Catalog Medicine"))
        .andExpect(jsonPath("$.data.prescription.items[1].medicineName").value("Custom Herbal"))
        .andExpect(jsonPath("$.data.prescription.items[1].medicineId").doesNotExist());
  }

  @Test
  void testPharmacyMultiSaleAndAutoBatchSelection() throws Exception {
    // 1. Setup: Medicine with multiple batches
    Medicine med = new Medicine();
    med.setName("Amoxicillin");
    medicineRepository.save(med);

    PharmacyStock batch1 = new PharmacyStock();
    batch1.setHospitalId(hospitalId);
    batch1.setMedicine(med);
    batch1.setBatchNumber("B1");
    batch1.setQuantity(10);
    batch1.setSellingPrice(new BigDecimal("5.00"));
    batch1.setExpiryDate(LocalDate.now().plusMonths(6));
    pharmacyStockRepository.save(batch1);

    PharmacyStock batch2 = new PharmacyStock();
    batch2.setHospitalId(hospitalId);
    batch2.setMedicine(med);
    batch2.setBatchNumber("B2");
    batch2.setQuantity(20);
    batch2.setSellingPrice(new BigDecimal("6.00"));
    batch2.setExpiryDate(LocalDate.now().plusMonths(12));
    pharmacyStockRepository.save(batch2);

    com.res.hms.clinical.domain.Patient patient = testDataHelper.createTestPatient(hospitalId);

    // 2. Create an Appointment and then a Prescription
    Appointment apt = new Appointment();
    apt.setHospitalId(hospitalId);
    apt.setPatientId(patient.getId());
    apt.setPatientName(patient.getFullName());
    apt.setDoctorId(doctorProfileId);
    apt.setAppointmentTime(Instant.now());
    apt.setStatus(AppointmentStatus.VITALS_RECORDED);
    apt.setTokenNumber(456);
    apt = appointmentRepository.save(apt);

    com.res.hms.clinical.domain.Prescription pres = new com.res.hms.clinical.domain.Prescription();
    pres.setHospitalId(hospitalId);
    pres.setPatientId(patient.getId());
    pres.setPatientName(patient.getFullName());
    pres.setDoctorId(doctorProfileId);
    pres.setDoctorName("Dr. Comprehensive");
    pres.setAppointment(apt);
    pres.setAppointmentId(apt.getId());
    pres.setStatus(com.res.hms.clinical.domain.PrescriptionStatus.ACTIVE);
    UUID prescriptionId = prescriptionRepository.save(pres).getId();

    // 3. First Sale: Specific Batch Selection (Quick Sale style)
    QuickPharmacySaleRequest sale1 = QuickPharmacySaleRequest.builder()
        .patientId(patient.getId())
        .prescriptionId(prescriptionId)
        .saleType(com.res.hms.pharmacy.domain.SaleType.VISIT)
        .items(List.of(
            QuickPharmacySaleRequest.Item.builder()
                .medicineBatchId(batch1.getId())
                .quantity(5)
                .unitPrice(new BigDecimal("5.00"))
                .build()
        ))
        .payment(QuickPharmacySaleRequest.PaymentInfo.builder().mode(com.res.hms.finance.domain.PaymentMode.CASH).amount(new BigDecimal("25.00")).build())
        .build();

    mockMvc.perform(post("/api/v1/pharmacy/sales")
            .session(pharmacistSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(sale1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].batchNumber").value("B1"))
        .andExpect(jsonPath("$.data.totalAmount").value(25.00));

    // 4. Second Sale: Auto-Batch Selection (Dispense style)
    // Should auto-pick remaining 5 from B1 and then move to B2 if needed
    // If we request 10, it should take 5 from B1 and 5 from B2
    QuickPharmacySaleRequest sale2 = QuickPharmacySaleRequest.builder()
        .patientId(patient.getId())
        .prescriptionId(prescriptionId)
        .items(List.of(
            QuickPharmacySaleRequest.Item.builder()
                .medicineId(med.getId())
                .quantity(10)
                .build()
        ))
        .payment(QuickPharmacySaleRequest.PaymentInfo.builder().mode(com.res.hms.finance.domain.PaymentMode.CASH).amount(new BigDecimal("55.00")).build())
        .build();

    mockMvc.perform(post("/api/v1/pharmacy/prescriptions/" + prescriptionId + "/dispense")
            .session(pharmacistSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(sale2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(2)) // 5 from B1, 5 from B2
        .andExpect(jsonPath("$.data.totalAmount").value(55.00)); // (5*5) + (5*6) = 25+30 = 55
  }
}
