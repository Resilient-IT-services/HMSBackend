package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockImportIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  private UUID testHospitalId;
  private MockHttpSession session;

  @BeforeEach
  void setup() throws Exception {
    testHospitalId = testDataHelper.createHospitalIfMissing("STOCK_TEST", "Stock Hospital");

    String username = "stock_admin_" + UUID.randomUUID().toString().substring(0, 8);
    UserAccount hospitalAdmin = new UserAccount();
    hospitalAdmin.setUsername(username);
    hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
    hospitalAdmin.setActive(true);
    hospitalAdmin.setSuperuser(true); // Superuser to allow medicine creation

    UserContract assignment = new UserContract();
    assignment.setUser(hospitalAdmin);
    assignment.setHospitalId(testHospitalId);
    assignment.setRole(roleRepository.findByCode(RoleCode.ADMIN).orElseThrow());
    hospitalAdmin.getContracts().add(assignment);

    userAccountRepository.save(hospitalAdmin);

    session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"username\": \"" + username + "\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
  }

  @Test
  void testStockImport() throws Exception {
    // 1. Create a Medicine first (Master record must exist)
    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"name\": \"ExcelMedicine\","
                        + "\"genericName\": \"ExcelMedicine\","
                        + "\"composition\": \"ExcelMedicine 500mg\","
                        + "\"strength\": \"500mg\","
                        + "\"form\": \"TABLET\","
                        + "\"purchasePrice\": 5.0,"
                        + "\"sellingPrice\": 10.0,"
                        + "\"manufacturer\": \"Generic Pharma\""
                        + "}"))
        .andExpect(status().isOk());

    // 2. Prepare Excel file
    Workbook workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet();
    var header = sheet.createRow(0);
    header.createCell(0).setCellValue("Medicine Name");
    header.createCell(1).setCellValue("Batch");
    header.createCell(2).setCellValue("Expiry");
    header.createCell(3).setCellValue("Qty");
    header.createCell(4).setCellValue("Purchase Price");
    header.createCell(5).setCellValue("Selling Price");

    var row = sheet.createRow(1);
    row.createCell(0).setCellValue("ExcelMedicine");
    row.createCell(1).setCellValue("B123");
    row.createCell(2).setCellValue("2026-12-31");
    row.createCell(3).setCellValue(100);
    row.createCell(4).setCellValue(5.0);
    row.createCell(5).setCellValue(10.0);

    var row2 = sheet.createRow(2);
    row2.createCell(0).setCellValue("UnknownMedicine");
    row2.createCell(1).setCellValue("B456");
    row2.createCell(2).setCellValue("2026-12-31");
    row2.createCell(3).setCellValue(50);
    row2.createCell(4).setCellValue(5.0);
    row2.createCell(5).setCellValue(10.0);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    byte[] content = bos.toByteArray();
    workbook.close();

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "stock.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content);

    // 3. Import
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/v1/pharmacy/stock/import")
                .file(file)
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalRows").value(2))
        .andExpect(jsonPath("$.data.importedRows").value(1))
        .andExpect(jsonPath("$.data.medicineNotFoundRows").value(1));
  }

  @Test
  void testDuplicateBatchAcrossDifferentMedicines() throws Exception {
    // 1. Create two medicines
    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"name\": \"MedicineA\","
                        + "\"genericName\": \"GenA\","
                        + "\"composition\": \"CompA\","
                        + "\"strength\": \"500mg\","
                        + "\"form\": \"TABLET\","
                        + "\"purchasePrice\": 5.0,"
                        + "\"sellingPrice\": 10.0,"
                        + "\"manufacturer\": \"MfA\""
                        + "}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/pharmacy/medicines")
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"name\": \"MedicineB\","
                        + "\"genericName\": \"GenB\","
                        + "\"composition\": \"CompB\","
                        + "\"strength\": \"500mg\","
                        + "\"form\": \"TABLET\","
                        + "\"purchasePrice\": 5.0,"
                        + "\"sellingPrice\": 10.0,"
                        + "\"manufacturer\": \"MfB\""
                        + "}"))
        .andExpect(status().isOk());

    // 2. Prepare Excel with duplicate batch for DIFFERENT medicines
    Workbook workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet();
    var header = sheet.createRow(0);
    header.createCell(0).setCellValue("Medicine Name");
    header.createCell(1).setCellValue("Batch");
    header.createCell(2).setCellValue("Expiry");
    header.createCell(3).setCellValue("Qty");
    header.createCell(4).setCellValue("Purchase Price");
    header.createCell(5).setCellValue("Selling Price");

    // First row: MedicineA, Batch B-DUP
    var row1 = sheet.createRow(1);
    row1.createCell(0).setCellValue("MedicineA");
    row1.createCell(1).setCellValue("B-DUP");
    row1.createCell(2).setCellValue("2026-12-31");
    row1.createCell(3).setCellValue(100);
    row1.createCell(4).setCellValue(10.0);
    row1.createCell(5).setCellValue(20.0);

    // Second row: MedicineB, Batch B-DUP (Should FAIL)
    var row2 = sheet.createRow(2);
    row2.createCell(0).setCellValue("MedicineB");
    row2.createCell(1).setCellValue("B-DUP");
    row2.createCell(2).setCellValue("2026-12-31");
    row2.createCell(3).setCellValue(50);
    row2.createCell(4).setCellValue(10.0);
    row2.createCell(5).setCellValue(20.0);

    // Third row: MedicineA, Batch B-DUP (Should be SKIPPED)
    var row3 = sheet.createRow(3);
    row3.createCell(0).setCellValue("MedicineA");
    row3.createCell(1).setCellValue("B-DUP");
    row3.createCell(2).setCellValue("2026-12-31");
    row3.createCell(3).setCellValue(30);
    row3.createCell(4).setCellValue(10.0);
    row3.createCell(5).setCellValue(20.0);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    byte[] content = bos.toByteArray();
    workbook.close();

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "stock_duplicates.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content);

    // 3. Import and verify results
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/v1/pharmacy/stock/import")
                .file(file)
                .session(session)
                .header("X-Hospital-Id", testHospitalId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalRows").value(3))
        .andExpect(jsonPath("$.data.importedRows").value(1)) // Only first row SUCCESS
        .andExpect(
            jsonPath("$.data.failedRows")
                .value(1)) // Second row FAIL (assigned to another medicine)
        .andExpect(
            jsonPath("$.data.skippedRows")
                .value(1)); // Third row SKIPPED (already exists for same medicine)
  }
}
