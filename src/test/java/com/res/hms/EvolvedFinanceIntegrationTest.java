package com.res.hms;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.clinical.domain.PatientVisit;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItem;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.finance.domain.InvoiceStatus;
import com.res.hms.finance.domain.Payment;
import com.res.hms.finance.domain.PaymentAllocation;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.finance.domain.PaymentStatus;
import com.res.hms.finance.domain.Refund;
import com.res.hms.finance.domain.RefundStatus;
import com.res.hms.finance.infrastructure.repository.InvoiceItemRepository;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import com.res.hms.finance.infrastructure.repository.PaymentAllocationRepository;
import com.res.hms.finance.infrastructure.repository.PaymentRepository;
import com.res.hms.finance.infrastructure.repository.RefundRepository;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EvolvedFinanceIntegrationTest {

    @Autowired private TestDataHelper testDataHelper;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private InvoiceItemRepository invoiceItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAllocationRepository paymentAllocationRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private UserAccountRepository userAccountRepository;

    private UUID hospitalId;
    private UUID patientId;
    private UUID userId;

    @BeforeEach
    void setup() {
        hospitalId = testDataHelper.createHospitalIfMissing("EVO_TEST", "Evolved Hospital");
        patientId = testDataHelper.createTestPatient(hospitalId).getId();
        
        UserAccount user = userAccountRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    UserAccount u = new UserAccount();
                    u.setUsername("test_user");
                    u.setPasswordHash("hash");
                    u.setActive(true);
                    return userAccountRepository.save(u);
                });
        userId = user.getId();
    }

    @Test
    void testEvolvedFinancePersistence() {
        // 1. Create a Visit
        PatientVisit visit = testDataHelper.createTestVisit(hospitalId, patientId);
        assertThat(visit.getId()).isNotNull();

        // 2. Create an Invoice
        Invoice invoice = new Invoice();
        invoice.setHospitalId(hospitalId);
        invoice.setPatientId(patientId);
        invoice.setVisitId(visit.getId());
        invoice.setInvoiceNumber("INV-001");
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setTotalAmount(new BigDecimal("1050.00"));
        invoice.setSubtotalAmount(new BigDecimal("1000.00"));
        invoice.setTaxAmount(new BigDecimal("50.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice = invoiceRepository.save(invoice);

        // 3. Add Invoice Item
        InvoiceItem item = new InvoiceItem();
        item.setHospitalId(hospitalId);
        item.setInvoiceId(invoice.getId());
        item.setItemDescription("Consultation");
        item.setCategory(InvoiceItemCategory.CONSULTATION);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setTaxPercentage(new BigDecimal("5.00"));
        item.setTaxAmount(new BigDecimal("50.00"));
        item.setDiscountAmount(BigDecimal.ZERO);
        item.setSubtotal(new BigDecimal("1000.00"));
        item.setNetTotal(new BigDecimal("1050.00"));
        item.setSourceType(FinanceSourceType.APPOINTMENT);
        item.setSourceId(UUID.randomUUID());
        invoiceItemRepository.save(item);

        // 4. Record a Payment
        Payment payment = new Payment();
        payment.setHospitalId(hospitalId);
        payment.setPatientId(patientId);
        payment.setReceiptNumber("RCP-001");
        payment.setAmount(new BigDecimal("600.00"));
        payment.setPaymentMode(PaymentMode.UPI);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(java.time.Instant.now());
        payment.setCollectedBy(userId);
        payment.setReferenceNumber("REF-123");
        payment = paymentRepository.save(payment);

        // 5. Allocate Payment to Invoice
        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setPaymentId(payment.getId());
        allocation.setInvoiceId(invoice.getId());
        allocation.setAllocatedAmount(new BigDecimal("600.00"));
        paymentAllocationRepository.save(allocation);

        // 6. Record a Refund
        Refund refund = new Refund();
        refund.setHospitalId(hospitalId);
        refund.setPaymentId(payment.getId());
        refund.setInvoiceId(invoice.getId());
        refund.setRefundAmount(new BigDecimal("100.00"));
        refund.setReason("Overcharged");
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setProcessedAt(java.time.Instant.now());
        refund.setSourceType(FinanceSourceType.LAB_ORDER);
        refund.setSourceId(UUID.randomUUID());
        refund = refundRepository.save(refund);

        assertThat(refund.getId()).isNotNull();
        assertThat(refund.getRefundAmount()).isEqualByComparingTo("100.00");
        assertThat(refund.getReason()).isEqualTo("Overcharged");
        assertThat(refund.getSourceType()).isEqualTo(FinanceSourceType.LAB_ORDER);

        // Verify entities are correctly linked
        Invoice savedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(savedInvoice.getInvoiceNumber()).isEqualTo("INV-001");
        assertThat(savedInvoice.getTotalAmount()).isEqualByComparingTo("1050.00");
        
        PaymentAllocation savedAllocation = paymentAllocationRepository.findAllByInvoiceId(invoice.getId()).get(0);
        assertThat(savedAllocation.getAllocatedAmount()).isEqualByComparingTo("600.00");
    }
}
