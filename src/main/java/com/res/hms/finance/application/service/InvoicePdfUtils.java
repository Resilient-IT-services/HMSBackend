package com.res.hms.finance.application.service;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.awt.Color;

public final class InvoicePdfUtils {
  public static final Color UI_ORANGE = new Color(241, 90, 34);
  public static final Color SOFT_ORANGE_BG = new Color(255, 238, 225);
  public static final Color BORDER_GREY = new Color(210, 200, 195);

  private InvoicePdfUtils() {}

  public static PdfPCell getTableHeaderSetup(String headerTitle, Font styleFont) {
    PdfPCell cell = new PdfPCell(new Phrase(headerTitle, styleFont));
    cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
    cell.setBorderColor(BORDER_GREY);
    cell.setBorderWidth(1f);
    cell.setPaddingTop(10);
    cell.setPaddingBottom(10);
    cell.setPaddingLeft(10);
    return cell;
  }

  public static void populateGridRow(
      PdfPTable mainGrid, String itemDesc, String totalNet, Font contentFont) {
    PdfPCell descCell = new PdfPCell(new Phrase(itemDesc, contentFont));
    descCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
    descCell.setBorderColor(BORDER_GREY);
    descCell.setPadding(10);
    mainGrid.addCell(descCell);

    PdfPCell netSumCell =
        new PdfPCell(new Phrase(totalNet, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
    netSumCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
    netSumCell.setBorderColor(BORDER_GREY);
    netSumCell.setPadding(10);
    mainGrid.addCell(netSumCell);
  }
}
