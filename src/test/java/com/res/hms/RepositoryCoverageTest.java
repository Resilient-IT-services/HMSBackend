package com.res.hms;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.clinical.infrastructure.repository.PatientVisitRepository;
import com.res.hms.finance.infrastructure.repository.InvoiceItemRepository;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import com.res.hms.finance.infrastructure.repository.PaymentAllocationRepository;
import com.res.hms.finance.infrastructure.repository.PaymentRepository;
import com.res.hms.finance.infrastructure.repository.RefundRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryCoverageTest {

  @Autowired private PatientVisitRepository patientVisitRepository;
  @Autowired private InvoiceRepository invoiceRepository;
  @Autowired private InvoiceItemRepository invoiceItemRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private PaymentAllocationRepository paymentAllocationRepository;
  @Autowired private RefundRepository refundRepository;

  @Test
  void testAllRepositories() {
    assertThat(patientVisitRepository.count()).isGreaterThanOrEqualTo(0);
    assertThat(invoiceRepository.count()).isGreaterThanOrEqualTo(0);
    assertThat(invoiceItemRepository.count()).isGreaterThanOrEqualTo(0);
    assertThat(paymentRepository.count()).isGreaterThanOrEqualTo(0);
    assertThat(paymentAllocationRepository.count()).isGreaterThanOrEqualTo(0);
    assertThat(refundRepository.count()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void testRepositoryLookups() {
    UUID randomId = UUID.randomUUID();
    UUID hospId = UUID.randomUUID();

    assertThat(patientVisitRepository.findByIdAndHospitalId(randomId, hospId)).isEmpty();
    assertThat(invoiceRepository.findByIdAndHospitalId(randomId, hospId)).isEmpty();
    assertThat(paymentRepository.findByIdAndHospitalId(randomId, hospId)).isEmpty();
    assertThat(refundRepository.findByIdAndHospitalId(randomId, hospId)).isEmpty();

    assertThat(patientVisitRepository.findAllByHospitalId(hospId)).isEmpty();
    assertThat(invoiceRepository.findAllByHospitalId(hospId)).isEmpty();
    assertThat(paymentRepository.findAllByHospitalId(hospId)).isEmpty();
    assertThat(refundRepository.findAllByHospitalId(hospId)).isEmpty();
  }
}
