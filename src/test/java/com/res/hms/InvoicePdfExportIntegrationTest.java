package com.res.hms;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItemCategory;
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
class InvoicePdfExportIntegrationTest {

  @Autowired private InvoiceService invoiceService;
  @Autowired private TestDataHelper testDataHelper;

  @Test
  void testExportInvoicePdf() {
    UUID hospitalId = testDataHelper.createHospitalIfMissing("PDF_TEST", "PDF Test Hospital");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

    Invoice invoice = invoiceService.createInvoice(hospitalId, patientId, null);
    invoiceService.addItem(
        invoice.getId(),
        "Consultation Fee",
        InvoiceItemCategory.CONSULTATION,
        BigDecimal.ONE,
        new BigDecimal("500.00"),
        BigDecimal.ZERO,
        com.res.hms.finance.domain.FinanceSourceType.TEST,
        UUID.randomUUID(),
        null,
        "Dr. Test",
        null);

    byte[] pdfContent = invoiceService.exportInvoicePdf(invoice.getId());

    assertThat(pdfContent).isNotNull();
    assertThat(pdfContent.length).isGreaterThan(0);
    // Basic check for PDF header %PDF-
    assertThat(new String(pdfContent, 0, 5)).isEqualTo("%PDF-");
  }
}
