package com.res.hms.finance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FinanceEnumTest {

    @Test
    void testInvoiceStatusEnum() {
        assertThat(InvoiceStatus.valueOf("PAID")).isEqualTo(InvoiceStatus.PAID);
        assertThat(InvoiceStatus.values()).hasSize(5);
    }

    @Test
    void testInvoiceItemCategoryEnum() {
        assertThat(InvoiceItemCategory.valueOf("PHARMACY")).isEqualTo(InvoiceItemCategory.PHARMACY);
        assertThat(InvoiceItemCategory.values()).hasSize(4);
    }

    @Test
    void testPaymentModeEnum() {
        assertThat(PaymentMode.valueOf("UPI")).isEqualTo(PaymentMode.UPI);
        assertThat(PaymentMode.values()).hasSize(5);
    }

    @Test
    void testPaymentStatusEnum() {
        assertThat(PaymentStatus.valueOf("SUCCESS")).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(PaymentStatus.values()).hasSize(3);
    }

    @Test
    void testRefundStatusEnum() {
        assertThat(RefundStatus.valueOf("COMPLETED")).isEqualTo(RefundStatus.COMPLETED);
        assertThat(RefundStatus.values()).hasSize(2);
    }
}
