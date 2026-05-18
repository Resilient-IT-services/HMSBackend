package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
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
                .content("{\"name\": \"ExcelMedicine\", \"strength\": \"500mg\"}"))
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
}
