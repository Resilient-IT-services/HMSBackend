package com.res.hms.finance.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

public class PdfHeaderFooter extends PdfPageEventHelper {
  private final String hospitalName;

  public PdfHeaderFooter(String hospitalName) {
    this.hospitalName = hospitalName;
  }

  @Override
  public void onStartPage(PdfWriter writer, Document document) {
    PdfContentByte backgroundCanvas = writer.getDirectContentUnder();
    backgroundCanvas.setColorFill(new Color(255, 252, 250));
    backgroundCanvas.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
    backgroundCanvas.fill();
  }

  @Override
  public void onEndPage(PdfWriter writer, Document document) {
    PdfContentByte cb = writer.getDirectContent();
    float completePageWidth = PageSize.A4.getWidth();
    float decorativeBandHeight = 55f;

    cb.setColorFill(new Color(255, 120, 70));
    cb.rectangle(0, 0, completePageWidth, decorativeBandHeight);
    cb.fill();

    Font thanksNoteFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    cb.beginText();
    cb.setFontAndSize(thanksNoteFont.getBaseFont(), 11);
    cb.setColorFill(Color.WHITE);
    cb.showTextAligned(
        Element.ALIGN_CENTER,
        "Thank you for choosing " + hospitalName + "!",
        completePageWidth / 2,
        24,
        0);
    cb.endText();
  }
}
