package com.res.hms.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.finance.domain.Payment;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.finance.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

  @Autowired private PaymentService paymentService;
  @Autowired private TestDataHelper testDataHelper;

  @Test
  void testRecordPayment() {
    UUID hospitalId =
        testDataHelper.createHospitalIfMissing("PAY_SERVICE_TEST", "Pay Service Test Hospital");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

    Payment payment =
        paymentService.recordPayment(
            hospitalId, patientId, new BigDecimal("500.00"), PaymentMode.CASH, UUID.randomUUID());
    assertThat(payment.getId()).isNotNull();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(payment.getAmount()).isEqualByComparingTo("500.00");
  }
}
