package com.res.hms.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.res.hms.TestDataHelper;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvoiceServiceTest {

  @Autowired private InvoiceService invoiceService;
  @Autowired private TestDataHelper testDataHelper;

  @Test
  void testCreateAndCancelInvoice() {
    UUID hospitalId =
        testDataHelper.createHospitalIfMissing("INV_SERVICE_TEST", "Inv Service Test Hospital");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

    Invoice invoice = invoiceService.createInvoice(hospitalId, patientId, null);
    assertAll(
        "Invoice creation checks",
        () -> assertThat(invoice.getId()).isNotNull(),
        () -> assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID));

    invoiceService.cancelInvoice(invoice.getId());
  }
}
