package com.res.hms.pharmacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItem;
import com.res.hms.finance.infrastructure.repository.InvoiceItemRepository;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.application.service.PharmacySaleService;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.domain.PharmacySaleItem;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.res.hms.TestDataHelper;
import com.res.hms.iam.application.security.HospitalUserPrincipal;
import java.util.Collections;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
@ActiveProfiles("test")
class PharmacyTaxIntegrationTest {

  @Autowired private PharmacySaleService pharmacySaleService;
  @Autowired private PharmacySaleRepository saleRepository;
  @Autowired private InvoiceRepository invoiceRepository;
  @Autowired private InvoiceItemRepository invoiceItemRepository;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private com.res.hms.core.tenant.HospitalContext hospitalContext;

  @Test
  @Transactional
  void testPharmacySaleWithMultipleTaxes() {
    // 1. Setup Hospital and Patient
    UUID hospitalId = testDataHelper.createHospitalIfMissing("PH_TAX_TEST", "Pharmacy Tax Hosp");
    com.res.hms.core.tenant.TenantContext.setHospitalId(hospitalId);
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();
    com.res.hms.clinical.domain.PatientVisit visit = testDataHelper.createTestVisit(hospitalId, patientId);
    UUID userId = UUID.randomUUID();

    // Mock Security Context
    HospitalUserPrincipal principal = new HospitalUserPrincipal(
        userId, "test_user", "test@test.com", "pass", true, false, 
        java.util.Collections.emptyMap(), hospitalId, java.util.Set.of());
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));

    // 2. Setup Medicine and Stock
    Medicine medicine = testDataHelper.createTestMedicine(hospitalId, "Taxo-Cillin", "500mg");
    medicine.setHsnCode("3004"); // Pharma HSN
    testDataHelper.addStock(hospitalId, medicine, 100, new BigDecimal("100.00"), "B123");

    // 3. Setup Multi-Tax Rule for Pharmacy
    testDataHelper.setupTaxRule("PHARMACY", List.of(
        new TestDataHelper.TaxComponentData("CGST_6", "Central GST 6%", "CGST", new BigDecimal("6.00")),
        new TestDataHelper.TaxComponentData("SGST_6", "State GST 6%", "SGST", new BigDecimal("6.00")),
        new TestDataHelper.TaxComponentData("CESS_1", "Health Cess 1%", "CESS", new BigDecimal("1.00")),
        new TestDataHelper.TaxComponentData("DRUG_FEE", "Drug Admin Fee 0.5%", "OTHER", new BigDecimal("0.50"))
    ));

    // 4. Create Sale Request
    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setPatientId(patientId);
    request.setVisitId(visit.getId());
    
    // Set states for Intra-state logic (CGST+SGST)
    // We'll mock the state resolution or ensure TaxEngineService sees them as same
    // Currently TaxEngineService takes them from TaxCalculationRequest
    // In PharmacySaleService, we don't yet fetch these from Hospital/Patient entities
    // So they default to null == null -> Intra-state.
    
    QuickPharmacySaleRequest.Item item = new QuickPharmacySaleRequest.Item();
    item.setMedicineId(medicine.getId());
    item.setQuantity(2); // Total Base = 200.00
    request.setItems(List.of(item));

    // 5. Execute
    var response = pharmacySaleService.createSale(request);

    // 6. Verify Sale Snapshots
    PharmacySale sale = saleRepository.findById(response.getSaleId()).orElseThrow();
    assertThat(sale.getItems()).hasSize(1);
    PharmacySaleItem saleItem = sale.getItems().get(0);
    
    // Calculations:
    // Taxable = 200.00
    // CGST (6%) = 12.00
    // SGST (6%) = 12.00
    // CESS (1%) = 2.00
    // DRUG_FEE (0.5%) = 1.00
    // Total Tax = 27.00
    // Grand Total = 227.00

    assertThat(saleItem.getTaxableAmount()).isEqualByComparingTo("200.00");
    assertThat(saleItem.getTaxes()).hasSize(4);
    assertThat(saleItem.getTaxAmount()).isEqualByComparingTo("27.00");
    assertThat(saleItem.getNetAmount()).isEqualByComparingTo("227.00");

    // 7. Verify Invoice Consolidation
    Invoice invoice = invoiceRepository.findByVisitId(sale.getVisitId()).orElseThrow();
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("227.00");
    assertThat(invoice.getTaxAmount()).isEqualByComparingTo("27.00");

    List<InvoiceItem> invItems = invoiceItemRepository.findAllByInvoiceId(invoice.getId());
    InvoiceItem invItem = invItems.get(0);
    assertThat(invItem.getTaxes()).hasSize(4);
    assertThat(invItem.getTaxAmount()).isEqualByComparingTo("27.00");
    
    // Check specific component (e.g., CESS)
    boolean hasCess = invItem.getTaxes().stream().anyMatch(t -> t.getTaxCode().equals("CESS_1"));
    assertThat(hasCess).isTrue();
  }
}
