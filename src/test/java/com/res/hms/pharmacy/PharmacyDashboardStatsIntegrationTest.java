package com.res.hms.pharmacy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.TestDataHelper;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PharmacyDashboardStatsIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private PharmacyStockRepository pharmacyStockRepository;

  @Test
  @Transactional
  void testGetDashboardStats() throws Exception {
    UUID hospitalId = testDataHelper.createHospitalIfMissing("STATS_HOSP", "Stats Hospital");
    com.res.hms.core.tenant.TenantContext.setHospitalId(hospitalId);

    // Mock Security Context
    HospitalUserPrincipal principal =
        new HospitalUserPrincipal(
            UUID.randomUUID(),
            "pharmacist",
            "pharm@test.com",
            "pass",
            true,
            false,
            Collections.emptyMap(),
            hospitalId,
            Set.of(RoleCode.PHARMACIST));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    // Create 2 Medicines
    var med1 = testDataHelper.createTestMedicine(hospitalId, "Med 1", "500mg");
    var med2 = testDataHelper.createTestMedicine(hospitalId, "Med 2", "100mg");

    // Add Stock
    // Batch 1 for Med 1 - Normal
    testDataHelper.addStock(hospitalId, med1, 100, new BigDecimal("10.00"), "B1");

    // Batch 2 for Med 1 - Low Stock (quantity 5, threshold is 10)
    testDataHelper.addStock(hospitalId, med1, 5, new BigDecimal("10.00"), "B2");

    // Batch 1 for Med 2 - Expiring Soon (10 days from now)
    var soon = LocalDate.now().plusDays(10);
    var stockSoon = testDataHelper.addStock(hospitalId, med2, 50, new BigDecimal("20.00"), "B3");
    stockSoon.setExpiryDate(soon);
    pharmacyStockRepository.saveAndFlush(stockSoon);

    // Total Batches: 3
    // Total Medicines: 2
    // Low Stock Count: 1 (B2)
    // Expiring Soon Count: 1 (B3)

    mockMvc
        .perform(get("/api/v1/pharmacy/dashboard/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalBatches").value(3))
        .andExpect(jsonPath("$.data.totalMedicines").value(2))
        .andExpect(jsonPath("$.data.lowStockCount").value(1))
        .andExpect(jsonPath("$.data.expiringSoonCount").value(1));
  }
}
