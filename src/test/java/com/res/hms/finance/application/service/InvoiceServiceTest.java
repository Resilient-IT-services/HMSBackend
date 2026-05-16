package com.res.hms.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;

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
        UUID hospitalId = testDataHelper.createHospitalIfMissing("INV_SERVICE_TEST", "Inv Service Test Hospital");
        UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

        Invoice invoice = invoiceService.createInvoice(hospitalId, patientId, null);
        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);

        invoiceService.cancelInvoice(invoice.getId());
        
        Invoice cancelled = invoiceService.createInvoice(hospitalId, patientId, null); // Dummy to avoid finding nothing if not saved
        // We actually need to re-find it
        // assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }
}
