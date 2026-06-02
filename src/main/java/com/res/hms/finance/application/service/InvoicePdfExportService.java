package com.res.hms.finance.application.service;

import static com.res.hms.finance.application.service.InvoicePdfUtils.BORDER_GREY;
import static com.res.hms.finance.application.service.InvoicePdfUtils.SOFT_ORANGE_BG;
import static com.res.hms.finance.application.service.InvoicePdfUtils.UI_ORANGE;
import static com.res.hms.finance.application.service.InvoicePdfUtils.getTableHeaderSetup;
import static com.res.hms.finance.application.service.InvoicePdfUtils.populateGridRow;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItem;
import com.res.hms.finance.domain.InvoiceItemCategory;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePdfExportService {

  private final InvoicePdfContextFactory contextFactory;

  public byte[] exportInvoicePdf(Invoice invoice) {
    return exportInvoicePdf(invoice, null);
  }

  public byte[] exportInvoicePdf(Invoice invoice, InvoiceItemCategory filterCategory) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4, 36, 36, 40, 90);
      PdfWriter writer = PdfWriter.getInstance(document, baos);
      InvoicePdfContext context = contextFactory.create(invoice);
      writer.setPageEvent(new PdfHeaderFooter(context.hospitalName()));
      document.open();

      // If filtering by category, we only want those items and their specific totals
      List<InvoiceItem> items = invoice.getItems();
      BigDecimal subtotal = invoice.getSubtotalAmount();
      BigDecimal discount = invoice.getDiscountAmount();
      BigDecimal tax = invoice.getTaxAmount();
      BigDecimal total = invoice.getTotalAmount();

      if (filterCategory != null) {
        items =
            items.stream()
                .filter(item -> filterCategory.equals(item.getCategory()))
                .collect(Collectors.toList());

        subtotal =
            items.stream().map(InvoiceItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        discount =
            items.stream()
                .map(InvoiceItem::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        tax =
            items.stream().map(InvoiceItem::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.add(tax).subtract(discount);
      }

      addBranding(document, context, invoice, filterCategory);
      addInfoGrid(document, invoice, context);
      addDivider(document);
      addSummaryMetrics(document, invoice, total);
      addDivider(document);
      addItemsGrid(document, items);
      addFinancialSummary(document, subtotal, discount, tax, total);
      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate invoice PDF: ", e);
      throw new RuntimeException("Failed to generate PDF", e);
    }
  }

  private void addBranding(
      Document document,
      InvoicePdfContext context,
      Invoice invoice,
      InvoiceItemCategory filterCategory)
      throws Exception {
    Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, UI_ORANGE);
    Font mainTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.BLACK);
    Font textBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    Font mutedLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(120, 120, 120));

    Paragraph bTitle = new Paragraph(context.hospitalName(), brandFont);
    bTitle.setAlignment(Element.ALIGN_CENTER);
    bTitle.setSpacingBefore(10);
    bTitle.setSpacingAfter(5);
    document.add(bTitle);

    addCenteredText(document, "GSTIN: " + context.gstinNumber(), textBold, 15);

    String invoiceTitle = "CONSULTATION BILLING INVOICE";
    if (InvoiceItemCategory.PHARMACY.equals(filterCategory)) {
      invoiceTitle = "PHARMACY BILLING RECEIPT";
    } else if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
      boolean allPharmacy =
          invoice.getItems().stream()
              .allMatch(
                  item ->
                      com.res.hms.finance.domain.InvoiceItemCategory.PHARMACY.equals(
                          item.getCategory()));
      if (allPharmacy) {
        invoiceTitle = "PHARMACY BILLING INVOICE";
      }
    }

    addCenteredText(document, invoiceTitle, mainTitleFont, 5);
    addCenteredText(document, "Billed By: " + context.receptionistName(), mutedLabel, 20);
  }

  private void addCenteredText(Document document, String text, Font font, float spacingAfter)
      throws Exception {
    Paragraph p = new Paragraph(text, font);
    p.setAlignment(Element.ALIGN_CENTER);
    p.setSpacingAfter(spacingAfter);
    document.add(p);
  }

  private void addInfoGrid(Document document, Invoice invoice, InvoicePdfContext context)
      throws Exception {
    Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, UI_ORANGE);
    Font textNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(50, 50, 50));
    Font mutedLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(120, 120, 120));
    Font patientNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);

    PdfPTable infoLayoutGrid = new PdfPTable(2);
    infoLayoutGrid.setWidthPercentage(100);
    infoLayoutGrid.setWidths(new float[] {50f, 50f});
    infoLayoutGrid.setSpacingAfter(20);

    PdfPCell patientCell = createInfoCell(sectionTitleFont, "PATIENT RECORD");
    Paragraph pNamePara = new Paragraph(context.patientName(), patientNameFont);
    pNamePara.setSpacingBefore(4);
    pNamePara.setSpacingAfter(4);
    patientCell.addElement(pNamePara);
    patientCell.addElement(new Paragraph("PATIENT ID: " + context.patientCode(), mutedLabel));
    infoLayoutGrid.addCell(patientCell);

    PdfPCell serviceCell = createInfoCell(sectionTitleFont, "DOCTOR RECORD");
    Paragraph docPara =
        new Paragraph("Consulting Physician: " + context.consultingDoctor(), textNormal);
    docPara.setSpacingBefore(4);
    serviceCell.addElement(docPara);

    if (invoice.getCreatedAt() != null) {
      String formattedDate = invoice.getCreatedAt().toString();
      try {
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());
        formattedDate = formatter.format(invoice.getCreatedAt());
      } catch (Exception ignored) {
      }
      serviceCell.addElement(new Paragraph("VISIT DATE: " + formattedDate, mutedLabel));
    }
    infoLayoutGrid.addCell(serviceCell);
    document.add(infoLayoutGrid);
  }

  private PdfPCell createInfoCell(Font sectionTitleFont, String title) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setCellEvent(
        (c, position, canvases) -> {
          PdfContentByte canvas = canvases[PdfPTable.BASECANVAS];
          canvas.setColorStroke(UI_ORANGE);
          canvas.setLineWidth(2.5f);
          canvas.moveTo(position.getLeft(), position.getTop());
          canvas.lineTo(position.getLeft(), position.getBottom());
          canvas.stroke();
        });
    cell.setPaddingLeft(12);
    cell.setPaddingBottom(12);
    cell.addElement(new Paragraph(title, sectionTitleFont));
    return cell;
  }

  private void addDivider(Document document) throws Exception {
    PdfPTable separatorLine = new PdfPTable(1);
    separatorLine.setWidthPercentage(100);
    PdfPCell lineWrapper = new PdfPCell();
    lineWrapper.setBorder(Rectangle.BOTTOM);
    lineWrapper.setBorderColor(BORDER_GREY);
    lineWrapper.setBorderWidth(1f);
    lineWrapper.setFixedHeight(1);
    separatorLine.addCell(lineWrapper);
    separatorLine.setSpacingAfter(20);
    document.add(separatorLine);
  }

  private void addSummaryMetrics(Document document, Invoice invoice, BigDecimal totalAmount)
      throws Exception {
    Font metaValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK);
    Font textBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

    PdfPTable metaRowTable = new PdfPTable(2);
    metaRowTable.setWidthPercentage(100);
    metaRowTable.setWidths(new float[] {50f, 50f});
    metaRowTable.setSpacingAfter(20);

    PdfPCell rCell1 = new PdfPCell();
    rCell1.setBorder(Rectangle.NO_BORDER);
    rCell1.addElement(new Paragraph("INVOICE NUMBER", textBold));
    rCell1.addElement(new Paragraph(invoice.getInvoiceNumber(), metaValueFont));
    metaRowTable.addCell(rCell1);

    PdfPCell rCell3 = new PdfPCell();
    rCell3.setBorder(Rectangle.NO_BORDER);
    rCell3.setBackgroundColor(SOFT_ORANGE_BG);
    rCell3.setPadding(12);
    rCell3.addElement(new Paragraph("TOTAL AMOUNT PAID", textBold));
    Paragraph balanceStr = new Paragraph("₹ " + totalAmount, metaValueFont);
    balanceStr.setSpacingBefore(2);
    rCell3.addElement(balanceStr);
    metaRowTable.addCell(rCell3);

    document.add(metaRowTable);
  }

  private void addItemsGrid(Document document, List<InvoiceItem> items) throws Exception {
    Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    Font textNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(50, 50, 50));

    PdfPTable detailsGrid = new PdfPTable(2);
    detailsGrid.setWidthPercentage(100);
    detailsGrid.setWidths(new float[] {70f, 30f});

    detailsGrid.addCell(getTableHeaderSetup("DESCRIPTION", thFont));
    detailsGrid.addCell(getTableHeaderSetup("TOTAL AMOUNT", thFont));

    if (items != null && !items.isEmpty()) {
      for (InvoiceItem item : items) {
        populateGridRow(
            detailsGrid, item.getItemDescription(), "₹ " + item.getNetTotal(), textNormal);
      }
    }

    PdfPCell structuralSpacer = new PdfPCell(new Phrase(" "));
    structuralSpacer.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
    structuralSpacer.setBorderColor(BORDER_GREY);
    structuralSpacer.setBorderWidth(1f);
    structuralSpacer.setFixedHeight(100f);
    detailsGrid.addCell(structuralSpacer);
    detailsGrid.addCell(structuralSpacer);

    detailsGrid.setSpacingAfter(20);
    document.add(detailsGrid);
  }

  private void addFinancialSummary(
      Document document, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total)
      throws Exception {
    Font textBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    PdfPTable financialSummaryTable = new PdfPTable(2);
    financialSummaryTable.setWidthPercentage(100);
    financialSummaryTable.setWidths(new float[] {55f, 45f});
    PdfPCell formattingSpacer = new PdfPCell();
    formattingSpacer.setBorder(Rectangle.NO_BORDER);
    financialSummaryTable.addCell(formattingSpacer);
    PdfPCell totalsContentCell = new PdfPCell();
    totalsContentCell.setBorder(Rectangle.NO_BORDER);
    PdfPTable mathematicalSubGrid = new PdfPTable(2);
    mathematicalSubGrid.setWidthPercentage(100);
    mathematicalSubGrid.setWidths(new float[] {55f, 45f});
    addCalculatedRow(mathematicalSubGrid, "Gross Base Fee Charges", "₹ " + subtotal, textBold);
    addCalculatedRow(mathematicalSubGrid, "Discount Deductions", "- ₹ " + discount, textBold);
    addCalculatedRow(mathematicalSubGrid, "Tax Component Values", "₹ " + tax, textBold);
    PdfPCell finalFooterLabel =
        new PdfPCell(
            new Phrase(
                "TOTAL PAID", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, UI_ORANGE)));
    finalFooterLabel.setBorder(Rectangle.NO_BORDER);
    finalFooterLabel.setBackgroundColor(SOFT_ORANGE_BG);
    finalFooterLabel.setPadding(10);
    finalFooterLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
    mathematicalSubGrid.addCell(finalFooterLabel);
    PdfPCell finalFooterValue =
        new PdfPCell(
            new Phrase(
                "₹ " + total, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, UI_ORANGE)));
    finalFooterValue.setBorder(Rectangle.NO_BORDER);
    finalFooterValue.setBackgroundColor(SOFT_ORANGE_BG);
    finalFooterValue.setPadding(10);
    finalFooterValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
    mathematicalSubGrid.addCell(finalFooterValue);
    totalsContentCell.addElement(mathematicalSubGrid);
    financialSummaryTable.addCell(totalsContentCell);
    document.add(financialSummaryTable);
  }

  private void addCalculatedRow(
      PdfPTable contextTable, String itemLabel, String monetaryValue, Font font) {
    PdfPCell matchingLabelCell = new PdfPCell(new Phrase(itemLabel, font));
    matchingLabelCell.setBorder(Rectangle.NO_BORDER);
    matchingLabelCell.setPaddingBottom(8);
    matchingLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
    contextTable.addCell(matchingLabelCell);
    PdfPCell matchingValueCell = new PdfPCell(new Phrase(monetaryValue, font));
    matchingValueCell.setBorder(Rectangle.NO_BORDER);
    matchingValueCell.setPaddingBottom(8);
    matchingValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    contextTable.addCell(matchingValueCell);
  }
}
