package com.res.hms.pharmacy.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscountValidationIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldRejectNegativeDiscountInPharmacySale() throws Exception {
    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setSaleType(SaleType.WALKIN);
    request.setCustomerName("Test Customer");

    QuickPharmacySaleRequest.Item item = new QuickPharmacySaleRequest.Item();
    item.setMedicineId(UUID.randomUUID());
    item.setQuantity(1);
    item.setUnitPrice(new BigDecimal("100.00"));
    item.setDiscountAmount(new BigDecimal("-10.00")); // Negative discount!

    request.setItems(List.of(item));

    QuickPharmacySaleRequest.PaymentInfo payment = new QuickPharmacySaleRequest.PaymentInfo();
    payment.setMode(PaymentMode.CASH);
    payment.setAmount(new BigDecimal("90.00"));
    request.setPayment(payment);

    mockMvc
        .perform(
            post("/api/v1/pharmacy/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "RECEPTIONIST")
  void shouldRejectNegativePaymentInOPDEncounter() throws Exception {
    com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest request =
        new com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest();
    request.setPatientId(UUID.randomUUID());
    request.setDoctorId(UUID.randomUUID());
    request.setAppointmentTime(java.time.OffsetDateTime.now());

    com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest.PaymentInfo payment =
        new com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest.PaymentInfo();
    payment.setMode(PaymentMode.CASH);
    payment.setAmount(new BigDecimal("-50.00")); // Negative payment!
    request.setPayment(payment);

    mockMvc
        .perform(
            post("/api/v1/appointments/book-consultation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
