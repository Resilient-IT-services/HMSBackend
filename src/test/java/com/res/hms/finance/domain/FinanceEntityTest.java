package com.res.hms.finance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinanceEntityTest {

  @Test
  void testInvoiceFields() {
    Invoice invoice = new Invoice();
    invoice.setPatientId(UUID.randomUUID());
    invoice.setVisitId(UUID.randomUUID());
    invoice.setInvoiceNumber("INV-123");
    invoice.setStatus(InvoiceStatus.PAID);
    invoice.setSubtotalAmount(BigDecimal.TEN);
    invoice.setTaxAmount(BigDecimal.ONE);
    invoice.setDiscountAmount(BigDecimal.ZERO);
    invoice.setTotalAmount(new BigDecimal("11.00"));
    invoice.setAmountPaid(new BigDecimal("11.00"));
    invoice.setDueDate(LocalDateTime.now());

    assertThat(invoice.getPatientId()).isNotNull();
    assertThat(invoice.getVisitId()).isNotNull();
    assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-123");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    assertThat(invoice.getSubtotalAmount()).isEqualTo(BigDecimal.TEN);
    assertThat(invoice.getTaxAmount()).isEqualTo(BigDecimal.ONE);
    assertThat(invoice.getDiscountAmount()).isEqualTo(BigDecimal.ZERO);
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("11.00");
    assertThat(invoice.getAmountPaid()).isEqualByComparingTo("11.00");
    assertThat(invoice.getDueDate()).isNotNull();
    assertThat(invoice.getItems()).isEmpty();
  }

  @Test
  void testInvoiceItemFields() {
    InvoiceItem item = new InvoiceItem();
    item.setInvoiceId(UUID.randomUUID());
    item.setItemDescription("Desc");
    item.setCategory(InvoiceItemCategory.LAB);
    item.setQuantity(BigDecimal.ONE);
    item.setUnitPrice(BigDecimal.TEN);
    item.setTaxAmount(BigDecimal.ZERO);
    item.setDiscountAmount(BigDecimal.ZERO);
    item.setSubtotal(BigDecimal.TEN);
    item.setNetTotal(BigDecimal.TEN);
    item.setSourceType(FinanceSourceType.APPOINTMENT);
    item.setSourceId(UUID.randomUUID());

    assertThat(item.getInvoiceId()).isNotNull();
    assertThat(item.getItemDescription()).isEqualTo("Desc");
    assertThat(item.getCategory()).isEqualTo(InvoiceItemCategory.LAB);
    assertThat(item.getQuantity()).isEqualTo(BigDecimal.ONE);
    assertThat(item.getUnitPrice()).isEqualTo(BigDecimal.TEN);
    assertThat(item.getTaxAmount()).isEqualTo(BigDecimal.ZERO);
    assertThat(item.getDiscountAmount()).isEqualTo(BigDecimal.ZERO);
    assertThat(item.getSubtotal()).isEqualTo(BigDecimal.TEN);
    assertThat(item.getNetTotal()).isEqualTo(BigDecimal.TEN);
    assertThat(item.getSourceType()).isEqualTo(FinanceSourceType.APPOINTMENT);
    assertThat(item.getSourceId()).isNotNull();
  }

  @Test
  void testPaymentFields() {
    Payment p = new Payment();
    p.setPatientId(UUID.randomUUID());
    p.setReceiptNumber("RCP");
    p.setAmount(BigDecimal.TEN);
    p.setPaymentMode(PaymentMode.CASH);
    p.setReferenceNumber("REF");
    p.setStatus(PaymentStatus.SUCCESS);
    p.setPaymentDate(java.time.Instant.now());
    p.setCollectedBy(UUID.randomUUID());

    assertThat(p.getPatientId()).isNotNull();
    assertThat(p.getReceiptNumber()).isEqualTo("RCP");
    assertThat(p.getAmount()).isEqualTo(BigDecimal.TEN);
    assertThat(p.getPaymentMode()).isEqualTo(PaymentMode.CASH);
    assertThat(p.getReferenceNumber()).isEqualTo("REF");
    assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(p.getPaymentDate()).isNotNull();
    assertThat(p.getCollectedBy()).isNotNull();
  }

  @Test
  void testRefundFields() {
    Refund r = new Refund();
    r.setPaymentId(UUID.randomUUID());
    r.setInvoiceId(UUID.randomUUID());
    r.setRefundAmount(BigDecimal.TEN);
    r.setReason("Reason");
    r.setStatus(RefundStatus.INITIATED);
    r.setSourceType(FinanceSourceType.APPOINTMENT);
    r.setSourceId(UUID.randomUUID());
    r.setProcessedAt(java.time.Instant.now());

    assertThat(r.getPaymentId()).isNotNull();
    assertThat(r.getInvoiceId()).isNotNull();
    assertThat(r.getRefundAmount()).isEqualTo(BigDecimal.TEN);
    assertThat(r.getReason()).isEqualTo("Reason");
    assertThat(r.getStatus()).isEqualTo(RefundStatus.INITIATED);
    assertThat(r.getSourceType()).isEqualTo(FinanceSourceType.APPOINTMENT);
    assertThat(r.getSourceId()).isNotNull();
    assertThat(r.getProcessedAt()).isNotNull();
  }

  @Test
  void testPaymentAllocationFields() {
    PaymentAllocation pa = new PaymentAllocation();
    pa.setPaymentId(UUID.randomUUID());
    pa.setInvoiceId(UUID.randomUUID());
    pa.setAllocatedAmount(BigDecimal.TEN);

    assertThat(pa.getPaymentId()).isNotNull();
    assertThat(pa.getInvoiceId()).isNotNull();
    assertThat(pa.getAllocatedAmount()).isEqualTo(BigDecimal.TEN);
  }
}
