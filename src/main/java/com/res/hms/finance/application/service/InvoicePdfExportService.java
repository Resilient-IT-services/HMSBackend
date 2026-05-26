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
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePdfExportService {

  private final InvoicePdfContextFactory contextFactory;

  public byte[] exportInvoicePdf(Invoice invoice) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4, 36, 36, 40, 90);
      PdfWriter writer = PdfWriter.getInstance(document, baos);
      InvoicePdfContext context = contextFactory.create(invoice);
      writer.setPageEvent(new PdfHeaderFooter(context.hospitalName()));
      document.open();
      addBranding(document, context);
      addInfoGrid(document, invoice, context);
      addDivider(document);
      addSummaryMetrics(document, invoice);
      addDivider(document);
      addItemsGrid(document, invoice);
      addFinancialSummary(document, invoice);
      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate invoice PDF: ", e);
      throw new RuntimeException("Failed to generate PDF", e);
    }
  }

  private void addBranding(Document document, InvoicePdfContext context) throws Exception {
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
    addCenteredText(document, "CONSULTATION BILLING INVOICE", mainTitleFont, 5);
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
    String patientIdStr =
        invoice.getPatientId() != null ? invoice.getPatientId().toString().substring(0, 8) : "N/A";
    patientCell.addElement(new Paragraph("PATIENT ID: " + patientIdStr, mutedLabel));
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

  private void addSummaryMetrics(Document document, Invoice invoice) throws Exception {
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
    Paragraph balanceStr = new Paragraph("₹ " + invoice.getTotalAmount(), metaValueFont);
    balanceStr.setSpacingBefore(2);
    rCell3.addElement(balanceStr);
    metaRowTable.addCell(rCell3);

    document.add(metaRowTable);
  }

  private void addItemsGrid(Document document, Invoice invoice) throws Exception {
    Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    Font textNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(50, 50, 50));

    PdfPTable detailsGrid = new PdfPTable(2);
    detailsGrid.setWidthPercentage(100);
    detailsGrid.setWidths(new float[] {70f, 30f});

    detailsGrid.addCell(getTableHeaderSetup("DESCRIPTION", thFont));
    detailsGrid.addCell(getTableHeaderSetup("TOTAL AMOUNT", thFont));

    if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
      for (InvoiceItem item : invoice.getItems()) {
        populateGridRow(
            detailsGrid, item.getItemDescription(), "₹ " + item.getNetTotal(), textNormal);
      }
    } else {
      populateGridRow(
          detailsGrid,
          "Consultation Base Fee Services",
          "₹ " + invoice.getSubtotalAmount(),
          textNormal);
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

  private void addFinancialSummary(Document document, Invoice invoice) throws Exception {
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
    addCalculatedRow(
        mathematicalSubGrid,
        "Gross Base Fee Charges",
        "₹ " + invoice.getSubtotalAmount(),
        textBold);
    addCalculatedRow(
        mathematicalSubGrid, "Discount Deductions", "- ₹ " + invoice.getDiscountAmount(), textBold);
    addCalculatedRow(
        mathematicalSubGrid, "Tax Component Values", "₹ " + invoice.getTaxAmount(), textBold);
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
                "₹ " + invoice.getTotalAmount(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, UI_ORANGE)));
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
