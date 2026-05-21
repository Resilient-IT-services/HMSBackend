package com.res.hms.pharmacy.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.TestDataHelper;
import com.res.hms.finance.application.dto.InvoiceDTO;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.SaleType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PharmacyDiscountPropagationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private InvoiceRepository invoiceRepository;

  @Autowired private com.res.hms.iam.infrastructure.repository.UserAccountRepository userAccountRepository;
  @Autowired private com.res.hms.iam.infrastructure.repository.RoleRepository roleRepository;
  @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  @Test
  void shouldPropagateDiscountToInvoice() throws Exception {
    UUID hospitalId = testDataHelper.createHospitalIfMissing("DISC_TEST", "Discount Test Hospital");
    
    // Create and login user
    testDataHelper.createUserIfMissing("pharmacy_admin", com.res.hms.iam.domain.RoleCode.ADMIN, hospitalId, "password123", passwordEncoder, userAccountRepository, roleRepository);
    
    org.springframework.mock.web.MockHttpSession session = (org.springframework.mock.web.MockHttpSession) mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"pharmacy_admin\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession();

    Medicine medicine = testDataHelper.createTestMedicine(hospitalId, "Paracetamol", "500mg");
    var stock = testDataHelper.addStock(hospitalId, medicine, 100, new BigDecimal("10.00"), "BATCH001");
    var patient = testDataHelper.createTestPatient(hospitalId);

    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setSaleType(SaleType.WALKIN);
    request.setPatientId(patient.getId());
    
    QuickPharmacySaleRequest.Item item = new QuickPharmacySaleRequest.Item();
    item.setMedicineId(medicine.getId());
    item.setMedicineBatchId(stock.getId());
    item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("10.00"));
    item.setDiscountAmount(new BigDecimal("5.00")); // 20.00 total - 5.00 discount = 15.00 net
    
    request.setItems(List.of(item));
    
    QuickPharmacySaleRequest.PaymentInfo payment = new QuickPharmacySaleRequest.PaymentInfo();
    payment.setMode(PaymentMode.CASH);
    payment.setAmount(new BigDecimal("15.00"));
    request.setPayment(payment);

    MvcResult result = mockMvc.perform(post("/api/v1/pharmacy/sales")
        .session(session)
        .header("X-Hospital-Id", hospitalId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String content = result.getResponse().getContentAsString();
    com.res.hms.core.api.ApiResponse<QuickPharmacySaleResponse> apiResponse = objectMapper.readValue(
        content, 
        objectMapper.getTypeFactory().constructParametricType(com.res.hms.core.api.ApiResponse.class, QuickPharmacySaleResponse.class));
    
    QuickPharmacySaleResponse response = apiResponse.data();

    try {
      assertThat(response.getTotalAmount()).isEqualByComparingTo("15.00");
    } catch (AssertionError e) {
      System.out.println("Response content: " + content);
      throw e;
    }

    // Verify Invoice
    Invoice invoice = invoiceRepository.findById(response.getInvoiceId()).orElseThrow();
    assertThat(invoice.getSubtotalAmount()).isEqualByComparingTo("20.00");
    assertThat(invoice.getDiscountAmount()).isEqualByComparingTo("5.00");
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("15.00");
    assertThat(invoice.getItems().get(0).getDiscountAmount()).isEqualByComparingTo("5.00");
  }
}
