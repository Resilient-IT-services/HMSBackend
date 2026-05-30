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
  void testOnAppointmentBookedCreatesChargeAndInvoiceWithCorrectAmounts() {
    // 1. Setup
    UUID hospitalId = testDataHelper.createHospitalIfMissing("CH_TEST", "Charge Hospital");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();
    
    // Create Doctor and set fee
    UUID doctorId = testDataHelper.createTestDoctor(hospitalId, "charge_dr_" + UUID.randomUUID().toString().substring(0, 8));
    java.math.BigDecimal expectedFee = new java.math.BigDecimal("1500.00");
    testDataHelper.setConsultationFee(doctorId, hospitalId, expectedFee);

    // Create an actual appointment in DB so listener can find it
    com.res.hms.clinical.domain.PatientVisit visit = testDataHelper.createTestVisit(hospitalId, patientId);
    Appointment appointment = testDataHelper.createTestAppointment(hospitalId, patientId, doctorId, visit.getId());

    AppointmentBookedEvent event = new AppointmentBookedEvent(appointment.getId(), patientId, hospitalId);

    // 2. Execute listener directly
    listener.onAppointmentBooked(event);

    // 3. Verify AppointmentCharge
    AppointmentCharge charge = chargeRepository.findByAppointmentIdAndHospitalId(appointment.getId(), hospitalId)
        .orElseThrow(() -> new AssertionError("AppointmentCharge was not created"));
    
    assertThat(charge.getAppointmentId()).isEqualTo(appointment.getId());
    assertThat(charge.getTotalAmount()).isEqualByComparingTo(expectedFee);
    assertThat(charge.getInvoiceId()).isNotNull();
    assertThat(charge.getItems()).hasSize(1);
    
    AppointmentChargeItem chargeItem = charge.getItems().get(0);
    assertThat(chargeItem.getItemName()).contains("Dr. Test");
    assertThat(chargeItem.getAmount()).isEqualByComparingTo(expectedFee);
    assertThat(chargeItem.getQuantity()).isEqualTo(1);
    assertThat(chargeItem.getSubtotal()).isEqualByComparingTo(expectedFee);

    // 4. Verify Invoice
    Invoice invoice = invoiceRepository.findById(charge.getInvoiceId()).orElseThrow();
    assertThat(invoice.getPatientId()).isEqualTo(patientId);
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo(expectedFee);
    
    // 5. Verify InvoiceItem
    java.util.List<com.res.hms.finance.domain.InvoiceItem> invoiceItems = invoiceItemRepository.findAllByInvoiceId(invoice.getId());
    assertThat(invoiceItems).hasSize(1);
    com.res.hms.finance.domain.InvoiceItem invoiceItem = invoiceItems.get(0);
    assertThat(invoiceItem.getItemDescription()).contains("Dr. Test");
    assertThat(invoiceItem.getNetTotal()).isEqualByComparingTo(expectedFee);
    assertThat(invoiceItem.getSourceType()).isEqualTo(com.res.hms.finance.domain.FinanceSourceType.APPOINTMENT);
    assertThat(invoiceItem.getSourceId()).isEqualTo(appointment.getId());
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
