package com.res.hms.pharmacy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.ImportResponseDto;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ConsumableImportServiceTest {

  @Mock private MedicineRepository repository;
  @Mock private HospitalMedicineCatalogRepository catalogRepository;
  @Mock private HospitalContext hospitalContext;
  @InjectMocks private ConsumableImportService importService;

  private UUID hospitalId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
  }

  private Workbook createValidWorkbook() {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();
    Row header = sheet.createRow(0);
    header.createCell(0).setCellValue("Department");
    header.createCell(1).setCellValue("Category");
    header.createCell(2).setCellValue("Item Code");
    header.createCell(3).setCellValue("Item Name");
    header.createCell(4).setCellValue("Base Unit");
    header.createCell(5).setCellValue("Manufacturer");
    header.createCell(6).setCellValue("Low Stock Alert");
    header.createCell(7).setCellValue("Is Batch Tracked");
    header.createCell(8).setCellValue("Is Expiry Tracked");
    return workbook;
  }

  @Test
  void testImportConsumables_Success() throws IOException {
    // Arrange
    Workbook workbook = createValidWorkbook();
    Sheet sheet = workbook.getSheetAt(0);
    Row row = sheet.createRow(1);
    row.createCell(0).setCellValue("PHARMACY");
    row.createCell(1).setCellValue("Syringe");
    row.createCell(2).setCellValue("CON001");
    row.createCell(3).setCellValue("Syringe 5ml");
    row.createCell(4).setCellValue("PCS");
    row.createCell(5).setCellValue("BD");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    byte[] content = bos.toByteArray();
    MultipartFile file =
        new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new ByteArrayInputStream(content));

    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(repository.findAllItemCodes()).thenReturn(Collections.emptySet());
    when(repository.findAllDeptItemNames()).thenReturn(Collections.emptySet());
    when(repository.save(any(Medicine.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    ImportResponseDto response = importService.importConsumables(file);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getTotalRows());
    assertEquals(1, response.getSuccessCount());
    assertEquals(0, response.getFailureCount());
    verify(repository, times(1)).save(any(Medicine.class));
  }

  @Test
  void testImportConsumables_DuplicateCode() throws IOException {
    // Arrange
    Workbook workbook = createValidWorkbook();
    Sheet sheet = workbook.getSheetAt(0);
    Row row = sheet.createRow(1);
    row.createCell(0).setCellValue("PHARMACY");
    row.createCell(1).setCellValue("Syringe");
    row.createCell(2).setCellValue("CON001"); // Duplicate Code
    row.createCell(3).setCellValue("Syringe 5ml");
    row.createCell(4).setCellValue("PCS");
    row.createCell(5).setCellValue("BD");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    MultipartFile file =
        new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            bos.toByteArray());

    when(repository.findAllItemCodes()).thenReturn(Set.of("CON001"));
    when(repository.findAllDeptItemNames()).thenReturn(Collections.emptySet());

    // Act
    ImportResponseDto response = importService.importConsumables(file);

    // Assert
    assertEquals(1, response.getTotalRows());
    assertEquals(0, response.getSuccessCount());
    assertEquals(1, response.getFailureCount());
    assertTrue(response.isErrorFileAvailable());
  }
}
