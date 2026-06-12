package com.res.hms.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItem;
import com.res.hms.finance.domain.InvoiceItemCategory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InvoicePdfExportServiceTest {

  @Mock private InvoicePdfContextFactory contextFactory;

  @InjectMocks private InvoicePdfExportService invoicePdfExportService;

  private Invoice invoice;
  private InvoicePdfContext context;

  @BeforeEach
  void setUp() {
    invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    invoice.setInvoiceNumber("INV-123");
    invoice.setSubtotalAmount(new BigDecimal("1000.00"));
    invoice.setDiscountAmount(new BigDecimal("100.00"));
    invoice.setTaxAmount(new BigDecimal("180.00"));
    invoice.setTotalAmount(new BigDecimal("1080.00"));
    invoice.setItems(new ArrayList<>());

    InvoiceItem item1 = new InvoiceItem();
    item1.setCategory(InvoiceItemCategory.CONSULTATION);
    item1.setItemDescription("Consultation Fee");
    item1.setSubtotal(new BigDecimal("500.00"));
    item1.setTaxAmount(new BigDecimal("90.00"));
    item1.setDiscountAmount(new BigDecimal("0.00"));
    item1.setNetTotal(new BigDecimal("590.00"));
    invoice.getItems().add(item1);

    InvoiceItem item2 = new InvoiceItem();
    item2.setCategory(InvoiceItemCategory.PHARMACY);
    item2.setItemDescription("Medicine A");
    item2.setSubtotal(new BigDecimal("500.00"));
    item2.setTaxAmount(new BigDecimal("90.00"));
    item2.setDiscountAmount(new BigDecimal("100.00"));
    item2.setNetTotal(new BigDecimal("490.00"));
    invoice.getItems().add(item2);

    context =
        new InvoicePdfContext(
            "TEST HOSPITAL",
            "GSTIN123",
            "Admin",
            "John Doe",
            "P-001",
            "9876543210",
            "Dr. Smith",
            false,
            UUID.randomUUID());

    when(contextFactory.create(any())).thenReturn(context);
  }

  @Test
  @DisplayName("PDF: Export full invoice should return byte array")
  void testExportFullInvoicePdf() {
    byte[] pdfData = invoicePdfExportService.exportInvoicePdf(invoice);
    assertNotNull(pdfData);
    assertTrue(pdfData.length > 0);
  }

  @Test
  @DisplayName("PDF: Export filtered invoice (Pharmacy only) should return byte array")
  void testExportFilteredInvoicePdf() {
    byte[] pdfData =
        invoicePdfExportService.exportInvoicePdf(invoice, InvoiceItemCategory.PHARMACY);
    assertNotNull(pdfData);
    assertTrue(pdfData.length > 0);
  }

  @Test
  @DisplayName("PDF: Export filtered invoice (Lab only - No items) should return byte array")
  void testExportFilteredInvoiceEmptyPdf() {
    byte[] pdfData = invoicePdfExportService.exportInvoicePdf(invoice, InvoiceItemCategory.LAB);
    assertNotNull(pdfData);
    assertTrue(pdfData.length > 0);
  }
}
