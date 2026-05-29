package com.res.hms.pharmacy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PharmacyExcelServiceTest {

  @Mock private MedicineRepository medicineRepository;
  @Mock private HospitalMedicineCatalogRepository hospitalMedicineCatalogRepository;
  @Mock private PharmacyStockRepository pharmacyStockRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private jakarta.persistence.EntityManager entityManager;

  @InjectMocks private PharmacyExcelService excelService;

  private UUID hospitalId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
  }

  @Test
  void testImportMedicines_Success() throws IOException {
    var workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet();
    var header = sheet.createRow(0);
    header.createCell(0).setCellValue("Name");
    header.createCell(1).setCellValue("Generic Name");
    header.createCell(2).setCellValue("Salt/Composition");
    header.createCell(3).setCellValue("Strength");
    header.createCell(4).setCellValue("Form");
    header.createCell(5).setCellValue("Manufacturer");
    header.createCell(6).setCellValue("Schedule");

    var row = sheet.createRow(1);
    row.createCell(0).setCellValue("New Med");
    row.createCell(1).setCellValue("Generic Name");
    row.createCell(2).setCellValue("Salt/Composition");
    row.createCell(3).setCellValue("500mg");
    row.createCell(4).setCellValue("TABLET");
    row.createCell(5).setCellValue("Manufacturer");
    row.createCell(6).setCellValue("OTC");

    var out = new ByteArrayOutputStream();
    workbook.write(out);
    workbook.close();

    var file =
        new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            out.toByteArray());

    when(medicineRepository.existsDuplicate(anyString(), any(), anyString(), any()))
        .thenReturn(false);

    var result = excelService.importMedicines(file);

    assertEquals(1, result.getImportedRows());
    assertEquals(0, result.getSkippedRows());
    verify(medicineRepository).save(any());
  }

  @Test
  void testImportStock_MedicineNotFound() throws IOException {
    var workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet();
    var header = sheet.createRow(0);
    header.createCell(0).setCellValue("Name");
    header.createCell(1).setCellValue("Batch");
    header.createCell(2).setCellValue("Expiry");
    header.createCell(3).setCellValue("Qty");
    header.createCell(4).setCellValue("PPrice");
    header.createCell(5).setCellValue("SPrice");

    var row = sheet.createRow(1);
    row.createCell(0).setCellValue("Unknown");
    row.createCell(1).setCellValue("B1");
    row.createCell(2).setCellValue("2026-12-31");
    row.createCell(3).setCellValue(10);
    row.createCell(4).setCellValue(5.0);
    row.createCell(5).setCellValue(10.0);

    var out = new ByteArrayOutputStream();
    workbook.write(out);
    workbook.close();

    var file =
        new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            out.toByteArray());

    when(medicineRepository.findByExactName("Unknown"))
        .thenReturn(java.util.Collections.emptyList());

    var result = excelService.importStock(file);

    assertEquals(0, result.getImportedRows());
    assertEquals(1, result.getMedicineNotFoundRows());
  }

  @Test
  void testImportStock_InvalidFile() {
    var file =
        new MockMultipartFile("file", "test.txt", "text/plain", "invalid content".getBytes());

    assertThrows(ApiException.class, () -> excelService.importStock(file));
  }
}
