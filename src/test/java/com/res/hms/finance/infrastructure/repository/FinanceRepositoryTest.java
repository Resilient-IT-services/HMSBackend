package com.res.hms.finance.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinanceRepositoryTest {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private TestDataHelper testDataHelper;

    @Test
    void testInvoiceRepositoryMethods() {
        UUID hospitalId = testDataHelper.createHospitalIfMissing("REPO_TEST", "Repo Hospital");
        UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

        Invoice invoice = new Invoice();
        invoice.setHospitalId(hospitalId);
        invoice.setPatientId(patientId);
        invoice.setInvoiceNumber("INV-REPO-001");
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setTotalAmount(BigDecimal.TEN);
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoiceRepository.save(invoice);

        Optional<Invoice> found = invoiceRepository.findByHospitalIdAndInvoiceNumber(hospitalId, "INV-REPO-001");
        assertThat(found).isPresent();

        Page<Invoice> page = invoiceRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();

        Optional<Invoice> byId = invoiceRepository.findByIdAndHospitalId(invoice.getId(), hospitalId);
        assertThat(byId).isPresent();
    }
}
