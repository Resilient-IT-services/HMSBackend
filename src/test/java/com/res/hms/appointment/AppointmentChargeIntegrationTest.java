package com.res.hms.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.application.event.AppointmentBookedEvent;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentCharge;
import com.res.hms.clinical.domain.AppointmentChargeItem;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentChargeRepository;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.finance.application.listener.FinancialIntegrationListener;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentChargeIntegrationTest {

  @Autowired private FinancialIntegrationListener listener;
  @Autowired private AppointmentChargeRepository chargeRepository;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private InvoiceRepository invoiceRepository;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private com.res.hms.clinical.infrastructure.repository.PatientVisitRepository visitRepository;

  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserAccountRepository userAccountRepository;

  @Autowired private com.res.hms.finance.infrastructure.repository.InvoiceItemRepository invoiceItemRepository;

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    invoiceItemRepository.deleteAll();
    chargeRepository.deleteAll();
    appointmentRepository.deleteAll();
    invoiceRepository.deleteAll();
    visitRepository.deleteAll();
    userAccountRepository.deleteAll();
  }

  @Test
  @Transactional
  void testOnAppointmentBookedWithMultipleTaxes() {
    // 1. Setup
    UUID hospitalId = testDataHelper.createHospitalIfMissing("CH_TAX_MULTI", "Multi Tax Hospital");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();
    
    // Create Doctor and set fee
    UUID doctorId = testDataHelper.createTestDoctor(hospitalId, "multi_dr_" + UUID.randomUUID().toString().substring(0, 8));
    java.math.BigDecimal expectedFee = new java.math.BigDecimal("1000.00");
    testDataHelper.setConsultationFee(doctorId, hospitalId, expectedFee);

    // Setup Multi-Tax Rule for CONSULTATION (CGST 9% + SGST 9%)
    testDataHelper.setupTaxRule("CONSULTATION", java.util.List.of(
        new com.res.hms.TestDataHelper.TaxComponentData("CGST_9", "Central GST 9%", "CGST", new java.math.BigDecimal("9.00")),
        new com.res.hms.TestDataHelper.TaxComponentData("SGST_9", "State GST 9%", "SGST", new java.math.BigDecimal("9.00"))
    ));

    // Create an actual appointment in DB
    com.res.hms.clinical.domain.PatientVisit visit = testDataHelper.createTestVisit(hospitalId, patientId);
    Appointment appointment = testDataHelper.createTestAppointment(hospitalId, patientId, doctorId, visit.getId());

    AppointmentBookedEvent event = new AppointmentBookedEvent(appointment.getId(), patientId, hospitalId);

    // 2. Execute
    listener.onAppointmentBooked(event);

    // 3. Verify
    AppointmentCharge charge = chargeRepository.findByAppointmentIdAndHospitalId(appointment.getId(), hospitalId).orElseThrow();
    
    // Expected: 1000 + 90 (CGST) + 90 (SGST) = 1180
    assertThat(charge.getTaxableAmount()).isEqualByComparingTo("1000.00");
    assertThat(charge.getTaxAmount()).isEqualByComparingTo("180.00");
    assertThat(charge.getTotalAmount()).isEqualByComparingTo("1180.00");
    assertThat(charge.getTaxes()).hasSize(2);

    // 4. Verify Invoice
    Invoice invoice = invoiceRepository.findById(charge.getInvoiceId()).orElseThrow();
    assertThat(invoice.getTaxAmount()).isEqualByComparingTo("180.00");
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("1180.00");
    
    java.util.List<com.res.hms.finance.domain.InvoiceItem> invoiceItems = invoiceItemRepository.findAllByInvoiceId(invoice.getId());
    assertThat(invoiceItems.get(0).getTaxes()).hasSize(2);
  }

  @Test
  @Transactional
  void testOnAppointmentBookedDeduplication() {
    // 1. Setup
    UUID hospitalId = testDataHelper.createHospitalIfMissing("CH_TEST_DEDUP", "Dedup Hospital");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();
    UUID doctorId = testDataHelper.createTestDoctor(hospitalId, "dedup_dr");
    
    com.res.hms.clinical.domain.PatientVisit visit = testDataHelper.createTestVisit(hospitalId, patientId);
    Appointment appointment = testDataHelper.createTestAppointment(hospitalId, patientId, doctorId, visit.getId());

    AppointmentBookedEvent event = new AppointmentBookedEvent(appointment.getId(), patientId, hospitalId);

    // 2. Execute twice
    listener.onAppointmentBooked(event);
    listener.onAppointmentBooked(event);

    // 3. Verify only ONE charge exists
    java.util.List<AppointmentCharge> charges = chargeRepository.findAll().stream()
        .filter(c -> c.getAppointmentId().equals(appointment.getId()))
        .toList();
    
    assertThat(charges).hasSize(1);
  }
}
