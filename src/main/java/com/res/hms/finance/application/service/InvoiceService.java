package com.res.hms.finance.application.service;

import java.awt.Color;
import com.lowagie.text.Phrase;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.res.hms.finance.application.dto.InvoiceDTO;
import com.res.hms.finance.application.dto.InvoiceItemDTO;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItem;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.finance.domain.InvoiceStatus;
import com.res.hms.finance.infrastructure.repository.InvoiceItemRepository;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final InvoiceItemRepository invoiceItemRepository;

  @Transactional(readOnly = true)
  public List<InvoiceDTO> getInvoicesByPatient(UUID patientId) {
    return invoiceRepository
        .findAll()
        .stream() // In real app, use a proper repo method with hospitalId
        .filter(i -> patientId.equals(i.getPatientId()))
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public InvoiceDTO getById(UUID id) {
    return invoiceRepository
        .findById(id)
        .map(this::toDto)
        .orElseThrow(() -> new RuntimeException("Invoice not found"));
  }

  @Transactional(readOnly = true)
  public InvoiceDTO getByVisit(UUID visitId) {
    return invoiceRepository
        .findByVisitId(visitId)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new com.res.hms.core.exception.ApiException(
                    "NOT_FOUND", "Invoice not found for visit", HttpStatus.NOT_FOUND));
  }

  public InvoiceDTO toDto(Invoice invoice) {
    return InvoiceDTO.builder()
        .id(invoice.getId())
        .patientId(invoice.getPatientId())
        .patientName(invoice.getPatient() != null ? invoice.getPatient().getFullName() : null)
        .visitId(invoice.getVisitId())
        .invoiceNumber(invoice.getInvoiceNumber())
        .status(invoice.getStatus())
        .subtotalAmount(invoice.getSubtotalAmount())
        .taxAmount(invoice.getTaxAmount())
        .discountAmount(invoice.getDiscountAmount())
        .totalAmount(invoice.getTotalAmount())
        .amountPaid(invoice.getAmountPaid())
        .amountDue(invoice.getAmountDue())
        .dueDate(invoice.getDueDate())
        .createdAt(invoice.getCreatedAt())
        .items(invoice.getItems().stream().map(this::toItemDto).collect(Collectors.toList()))
        .build();
  }

  private InvoiceItemDTO toItemDto(InvoiceItem item) {
    return InvoiceItemDTO.builder()
        .id(item.getId())
        .itemDescription(item.getItemDescription())
        .category(item.getCategory())
        .quantity(item.getQuantity())
        .unitPrice(item.getUnitPrice())
        .subtotal(item.getSubtotal())
        .taxAmount(item.getTaxAmount())
        .discountAmount(item.getDiscountAmount())
        .netTotal(item.getNetTotal())
        .sourceType(item.getSourceType())
        .sourceId(item.getSourceId())
        .build();
  }

  @Transactional
  public Invoice getOrCreateInvoice(UUID hospitalId, UUID patientId, UUID visitId) {
    if (visitId != null) {
      return invoiceRepository
          .findByVisitId(visitId)
          .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
          .orElseGet(() -> createInvoice(hospitalId, patientId, visitId));
    }
    return createInvoice(hospitalId, patientId, visitId);
  }

  @Transactional
  public Invoice createInvoice(UUID hospitalId, UUID patientId, UUID visitId) {
    Invoice invoice = new Invoice();
    invoice.setHospitalId(hospitalId);
    invoice.setPatientId(patientId);
    invoice.setVisitId(visitId);
    invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
    invoice.setStatus(InvoiceStatus.UNPAID);
    invoice.setSubtotalAmount(BigDecimal.ZERO);
    invoice.setTaxAmount(BigDecimal.ZERO);
    invoice.setDiscountAmount(BigDecimal.ZERO);
    invoice.setTotalAmount(BigDecimal.ZERO);
    invoice.setAmountPaid(BigDecimal.ZERO);
    Invoice saved = invoiceRepository.saveAndFlush(invoice);
    return saved;
  }

  @Transactional
  public void addItem(
      UUID invoiceId,
      String description,
      InvoiceItemCategory category,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal taxAmount,
      BigDecimal discountAmount,
      com.res.hms.finance.domain.FinanceSourceType sourceType,
      UUID sourceId,
      UUID doctorId,
      String doctorName) {
    Invoice invoice =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found"));

    InvoiceItem item = new InvoiceItem();
    item.setHospitalId(invoice.getHospitalId());
    item.setInvoiceId(invoiceId);
    item.setItemDescription(description);
    item.setCategory(category);
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);
    item.setTaxPercentage(BigDecimal.ZERO);
    item.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    item.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    item.setSubtotal(unitPrice.multiply(quantity));
    item.setNetTotal(item.getSubtotal().add(item.getTaxAmount()).subtract(item.getDiscountAmount()));
    item.setSourceType(sourceType);
    item.setSourceId(sourceId);
    item.setDoctorId(doctorId);
    item.setDoctorName(doctorName);
    item.setInvoice(invoice);

    invoiceItemRepository.save(item);
    invoice.getItems().add(item);

    updateTotals(invoice);
  }

  @Transactional
  public void updateTotals(Invoice invoice) {
    BigDecimal subtotal =
        invoice.getItems().stream()
            .map(InvoiceItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal tax =
        invoice.getItems().stream()
            .map(InvoiceItem::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal discount =
        invoice.getItems().stream()
            .map(InvoiceItem::getDiscountAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    invoice.setSubtotalAmount(subtotal);
    invoice.setTaxAmount(tax);
    invoice.setDiscountAmount(discount);
    invoice.setTotalAmount(subtotal.add(tax).subtract(discount));

    invoiceRepository.save(invoice);
  }

  @Transactional
  public void cancelInvoice(UUID invoiceId) {
    invoiceRepository
        .findById(invoiceId)
        .ifPresent(
            invoice -> {
              invoice.setStatus(InvoiceStatus.CANCELLED);
              invoiceRepository.save(invoice);
            });
  }
    @Transactional(readOnly = true)
    public byte[] exportInvoicePdf(UUID invoiceId) {

        try {

            Invoice invoice =
                invoiceRepository
                    .findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            String patientName =
                invoice.getPatient() != null
                    ? invoice.getPatient().getFullName()
                    : "Unknown";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(document, baos);

            document.open();


            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);


            Paragraph hospitalTitle = new Paragraph("CAREFLOW HOSPITAL", titleFont);
            hospitalTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(hospitalTitle);

            Paragraph subtitle = new Paragraph("Consultation Invoice", normalFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(25);
            document.add(subtitle);


            PdfPTable infoTable = new PdfPTable(2);

            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);

            infoTable.addCell(getCell("Invoice Number", boldFont));
            infoTable.addCell(getCell(invoice.getInvoiceNumber(), normalFont));

            infoTable.addCell(getCell("Patient Name", boldFont));
            infoTable.addCell(getCell(patientName, normalFont));

            infoTable.addCell(getCell("Invoice Status", boldFont));
            infoTable.addCell(getCell(invoice.getStatus().name(), normalFont));

            infoTable.addCell(getCell("Total Amount", boldFont));
            infoTable.addCell(getCell("₹ " + invoice.getTotalAmount(), normalFont));

            document.add(infoTable);


            PdfPTable billingTable = new PdfPTable(4);

            billingTable.setWidthPercentage(100);
            billingTable.setSpacingBefore(10);

            billingTable.addCell(getHeaderCell("Description"));
            billingTable.addCell(getHeaderCell("Qty"));
            billingTable.addCell(getHeaderCell("Price"));
            billingTable.addCell(getHeaderCell("Total"));

            billingTable.addCell(getCell("Consultation Fee", normalFont));
            billingTable.addCell(getCell("1", normalFont));
            billingTable.addCell(getCell("₹ " + invoice.getSubtotalAmount(), normalFont));
            billingTable.addCell(getCell("₹ " + invoice.getTotalAmount(), boldFont));

            document.add(billingTable);


            Paragraph footer = new Paragraph(
                "\nThank you for choosing CareFlow Hospital.",
                normalFont
            );

            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(40);

            document.add(footer);

            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
    private PdfPCell getCell(String text, Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(text, font));

        cell.setPadding(10);
        cell.setBorderWidth(0.5f);

        return cell;
    }

    private PdfPCell getHeaderCell(String text) {

        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        font.setSize(12);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));

        cell.setBackgroundColor(new Color(255, 102, 0));

        cell.setPadding(10);

        return cell;
    }
}
