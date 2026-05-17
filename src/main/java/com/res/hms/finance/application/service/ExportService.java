package com.res.hms.finance.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.res.hms.finance.application.dto.PaymentSummaryDTO;
import com.res.hms.finance.application.dto.RevenueReportDTO;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

  public byte[] generateExcel(ReportType type, List<?> data, LocalDate start, LocalDate end)
      throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Report");

      // Header Style
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      org.apache.poi.ss.usermodel.Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);

      Row headerRow = sheet.createRow(0);
      String[] headers =
          (type == ReportType.DOCTOR_REVENUE || type == ReportType.PHARMACY_REVENUE)
              ? new String[] {type == ReportType.PHARMACY_REVENUE ? "Category" : "Doctor Name", "Total Items/Appointments", "Total Revenue"}
              : new String[] {type == ReportType.PAYMENT_COLLECTOR_SUMMARY ? "Collector Name" : "Payment Method", "Transaction Count", "Total Amount"};

      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIdx = 1;
      if (type == ReportType.DOCTOR_REVENUE || type == ReportType.PHARMACY_REVENUE) {
        for (Object obj : data) {
          RevenueReportDTO dto = (RevenueReportDTO) obj;
          Row row = sheet.createRow(rowIdx++);
          row.createCell(0).setCellValue(dto.getDoctorName());
          row.createCell(1).setCellValue(dto.getTotalAppointments());
          row.createCell(2).setCellValue(dto.getTotalRevenue().doubleValue());
        }
      } else {
        for (Object obj : data) {
          PaymentSummaryDTO dto = (PaymentSummaryDTO) obj;
          Row row = sheet.createRow(rowIdx++);
          row.createCell(0).setCellValue(dto.getPaymentMethod());
          row.createCell(1).setCellValue(dto.getTransactionCount());
          row.createCell(2).setCellValue(dto.getTotalAmount().doubleValue());
        }
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }

  public byte[] generatePdf(ReportType type, List<?> data, LocalDate start, LocalDate end) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4);
    PdfWriter.getInstance(document, out);

    document.open();
    Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
    fontTitle.setSize(18);

    Paragraph title = new Paragraph(type.name().replace("_", " ") + " REPORT", fontTitle);
    title.setAlignment(Paragraph.ALIGN_CENTER);
    document.add(title);

    Paragraph dateRange = new Paragraph("Period: " + start + " to " + end);
    dateRange.setAlignment(Paragraph.ALIGN_CENTER);
    document.add(dateRange);
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);

    // Header cells
    PdfPCell headerCell = new PdfPCell();
    headerCell.setBackgroundColor(Color.LIGHT_GRAY);
    headerCell.setPadding(5);

    Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
    String[] headers =
        (type == ReportType.DOCTOR_REVENUE || type == ReportType.PHARMACY_REVENUE)
            ? new String[] {type == ReportType.PHARMACY_REVENUE ? "Category" : "Doctor Name", "Total Items/Appointments", "Total Revenue"}
            : new String[] {type == ReportType.PAYMENT_COLLECTOR_SUMMARY ? "Collector Name" : "Payment Method", "Transaction Count", "Total Amount"};

    for (String header : headers) {
      headerCell.setPhrase(new Phrase(header, headFont));
      table.addCell(headerCell);
    }

    if (type == ReportType.DOCTOR_REVENUE || type == ReportType.PHARMACY_REVENUE) {
      for (Object obj : data) {
        RevenueReportDTO dto = (RevenueReportDTO) obj;
        table.addCell(dto.getDoctorName());
        table.addCell(String.valueOf(dto.getTotalAppointments()));
        table.addCell(String.valueOf(dto.getTotalRevenue()));
      }
    } else {
      for (Object obj : data) {
        PaymentSummaryDTO dto = (PaymentSummaryDTO) obj;
        table.addCell(dto.getPaymentMethod());
        table.addCell(String.valueOf(dto.getTransactionCount()));
        table.addCell(String.valueOf(dto.getTotalAmount()));
      }
    }

    document.add(table);
    document.close();

    return out.toByteArray();
  }
}
