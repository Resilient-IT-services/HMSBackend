package com.res.hms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.Payment;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import com.res.hms.finance.infrastructure.repository.PaymentRepository;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.pharmacy.application.dto.PharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.domain.SaleType;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UnifiedFinancialWorkflowIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private InvoiceRepository invoiceRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  @Autowired private MedicineRepository medicineRepository;
  @Autowired private PharmacyStockRepository pharmacyStockRepository;
  @Autowired
  private com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository
      doctorScheduleRepository;
  @Autowired
  private com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityOverrideRepository
      doctorAvailabilityOverrideRepository;

  @Autowired
  private com.res.hms.clinical.infrastructure.repository.PatientVisitRepository
      patientVisitRepository;

  @Autowired
  private com.res.hms.clinical.infrastructure.repository.AppointmentRepository
      appointmentRepository;

  @Autowired
  private com.res.hms.finance.infrastructure.repository.InvoiceItemRepository invoiceItemRepository;

  private UUID hospitalId;
  private UUID patientId;
  private UUID doctorId;
  private UUID userId;
  private MockHttpSession session;

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    invoiceItemRepository.deleteAll();
    invoiceRepository.deleteAll();
    paymentRepository.deleteAll();
    appointmentRepository.deleteAll();
    patientVisitRepository.deleteAll();
    doctorAvailabilityOverrideRepository.deleteAll();
    doctorScheduleRepository.deleteAll();
    doctorHospitalDetailsRepository.deleteAll();
    userAccountRepository.deleteAll();
  }

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("UNIFIED_TEST", "Unified Hospital");
    patientId = testDataHelper.createTestPatient(hospitalId).getId();

    // 1. Create Staff User
    UserAccount user = new UserAccount();
    user.setUsername("admin_test_" + UUID.randomUUID().toString().substring(0, 4));
    user.setFullName("Admin Test");
    user.setPasswordHash(passwordEncoder.encode("password"));
    user.setActive(true);

    UserContract assignRec = new UserContract();
    assignRec.setUser(user);
    assignRec.setHospitalId(hospitalId);
    assignRec.setRole(roleRepository.findByCode(RoleCode.RECEPTIONIST).orElseThrow());
    user.getContracts().add(assignRec);

    UserContract assignPharm = new UserContract();
    assignPharm.setUser(user);
    assignPharm.setHospitalId(hospitalId);
    assignPharm.setRole(roleRepository.findByCode(RoleCode.PHARMACIST).orElseThrow());
    user.getContracts().add(assignPharm);

    user = userAccountRepository.save(user);
    userId = user.getId();

    // 2. Create Doctor
    UserAccount drAccount = new UserAccount();
    drAccount.setUsername("doctor_test_" + UUID.randomUUID().toString().substring(0, 4));
    drAccount.setFullName("Doctor Test");
    drAccount.setPasswordHash(passwordEncoder.encode("password"));
    drAccount.setActive(true);

    DoctorProfile drProfile = new DoctorProfile();
    drProfile.setUser(drAccount);
    com.res.hms.iam.domain.Specialization spec = new com.res.hms.iam.domain.Specialization();
    spec.setName("General");
    drProfile.getSpecializations().add(spec);
    drAccount.setDoctorProfile(drProfile);

    UserContract drAssign = new UserContract();
    drAssign.setUser(drAccount);
    drAssign.setHospitalId(hospitalId);
    drAssign.setRole(roleRepository.findByCode(RoleCode.DOCTOR).orElseThrow());
    drAccount.getContracts().add(drAssign);

    drAccount = userAccountRepository.save(drAccount);
    doctorId = drAccount.getDoctorProfile().getId();

    com.res.hms.clinical.domain.DoctorSchedule schedule =
        new com.res.hms.clinical.domain.DoctorSchedule();
    schedule.setAssignmentId(drAccount.getContracts().iterator().next().getId());
    schedule.setDayOfWeek(java.time.LocalDate.now().plusDays(1).getDayOfWeek());
    schedule.setStartTime(java.time.LocalTime.of(9, 0));
    schedule.setEndTime(java.time.LocalTime.of(11, 0));
    doctorScheduleRepository.save(schedule);

    com.res.hms.clinical.domain.DoctorAvailabilityOverride override =
        new com.res.hms.clinical.domain.DoctorAvailabilityOverride();
    override.setDoctorId(doctorId);
    override.setHospitalId(hospitalId);
    override.setDate(java.time.LocalDate.now().plusDays(1));
    override.setType(com.res.hms.clinical.domain.ExceptionType.ADDED);
    override.setStartTime(java.time.LocalTime.of(9, 0));
    override.setEndTime(java.time.LocalTime.of(11, 0));
    doctorAvailabilityOverrideRepository.save(override);

    DoctorHospitalDetails drDetails = new DoctorHospitalDetails();
    drDetails.setAssignment(drAccount.getContracts().iterator().next());
    drDetails.setConsultationFee(new BigDecimal("500.00"));
    doctorHospitalDetailsRepository.save(drDetails);

    // Login
    String loginBody = "{\"username\": \"" + user.getUsername() + "\", \"password\": \"password\"}";
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();
    session = (MockHttpSession) loginResult.getRequest().getSession();
  }

  @Test
  void testAtomicOPDBookingWithPayment() throws Exception {
    OPDEncounterRequest request = new OPDEncounterRequest();
    request.setPatientId(patientId);
    request.setDoctorId(doctorId);
    request.setAppointmentTime(
        java.time.OffsetDateTime.now()
            .plusDays(1)
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0));

    OPDEncounterRequest.PaymentInfo paymentInfo = new OPDEncounterRequest.PaymentInfo();
    paymentInfo.setAmount(new BigDecimal("500.00"));
    paymentInfo.setMode(PaymentMode.CASH);
    paymentInfo.setReferenceNumber("CASH-001");
    request.setPayment(paymentInfo);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/appointments/book-consultation")
                    .session(session)
                    .header("X-Hospital-Id", hospitalId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    UUID visitId =
        UUID.fromString(objectMapper.readTree(content).path("data").path("visitId").asText());

    // Verify Invoice
    List<Invoice> invoices = invoiceRepository.findAllByHospitalId(hospitalId);
    assertThat(invoices).hasSize(1);
    assertThat(invoices.get(0).getTotalAmount()).isEqualByComparingTo("500.00");
    assertThat(invoices.get(0).getStatus())
        .isEqualTo(com.res.hms.finance.domain.InvoiceStatus.PAID);

    // Verify Payment
    List<Payment> payments = paymentRepository.findAllByHospitalId(hospitalId);
    assertThat(payments).hasSize(1);
    assertThat(payments.get(0).getAmount()).isEqualByComparingTo("500.00");
    assertThat(payments.get(0).getVisitId()).isEqualTo(visitId);
  }

  @Test
  void testPharmacyWalkInQuickSale() throws Exception {
    // Setup Medicine
    Medicine med = new Medicine();
    med.setName("Walkin Med");
    med = medicineRepository.save(med);

    PharmacyStock stock = new PharmacyStock();
    stock.setHospitalId(hospitalId);
    stock.setMedicine(med);
    stock.setBatchNumber("W-001");
    stock.setQuantity(10);
    stock.setSellingPrice(new BigDecimal("200.00"));
    pharmacyStockRepository.save(stock);

    PharmacySaleRequest saleReq = new PharmacySaleRequest();
    saleReq.setPatientId(patientId);
    PharmacySaleRequest.Item item = new PharmacySaleRequest.Item();
    item.setMedicineId(med.getId());
    item.setQuantity(1);
    saleReq.setItems(List.of(item));

    mockMvc
        .perform(
            post("/api/v1/pharmacy/sales")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleReq)))
        .andExpect(status().isOk());

    // Verify that a WALK-IN visit was created and an invoice generated
    List<Invoice> invoices = invoiceRepository.findAllByHospitalId(hospitalId);
    assertThat(invoices).hasSize(1);
    assertThat(invoices.get(0).getTotalAmount()).isEqualByComparingTo("200.00");
  }

  @Test
  void testQuickPharmacyWalkinSale() throws Exception {
    // 1. Setup Medicine & Stock
    Medicine med = new Medicine();
    med.setName("Quick Med");
    med = medicineRepository.save(med);

    PharmacyStock stock = new PharmacyStock();
    stock.setHospitalId(hospitalId);
    stock.setMedicine(med);
    stock.setBatchNumber("Q-001");
    stock.setQuantity(100);
    stock.setSellingPrice(new BigDecimal("150.00"));
    stock = pharmacyStockRepository.save(stock);

    // 2. Prepare Quick Sale Request
    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setSaleType(SaleType.WALKIN);
    request.setCustomerName("Walkin Customer");
    request.setCustomerPhone("1234567890");
    request.setExternalDoctorName("Dr. External");

    QuickPharmacySaleRequest.Item item = new QuickPharmacySaleRequest.Item();
    item.setMedicineId(med.getId());
    item.setMedicineBatchId(stock.getId());
    item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("150.00"));
    request.setItems(List.of(item));

    QuickPharmacySaleRequest.PaymentInfo payment = new QuickPharmacySaleRequest.PaymentInfo();
    payment.setAmount(new BigDecimal("300.00"));
    payment.setMode(PaymentMode.CASH);
    request.setPayment(payment);

    // 3. Perform Request
    mockMvc
        .perform(
            post("/api/v1/pharmacy/sales")
                .session(session)
                .header("X-Hospital-Id", hospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.saleNumber").exists())
        .andExpect(jsonPath("$.data.totalAmount").value(300.00));

    // 4. Verify Financials
    List<Invoice> invoices = invoiceRepository.findAllByHospitalId(hospitalId);
    // We might have invoices from other tests if not cleaned properly, but @Transactional should
    // handle it.
    // Actually, we expect 1 here if it's the only test running in this tx.
    assertThat(invoices)
        .anyMatch(inv -> inv.getTotalAmount().compareTo(new BigDecimal("300.00")) == 0);

    List<Payment> payments = paymentRepository.findAllByHospitalId(hospitalId);
    assertThat(payments).anyMatch(p -> p.getAmount().compareTo(new BigDecimal("300.00")) == 0);
  }
}
