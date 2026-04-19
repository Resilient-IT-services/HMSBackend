package com.res.hms.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.res.hms.billing.dto.AddBillingItemRequest;
import com.res.hms.billing.dto.CreateOpdBillingRequest;
import com.res.hms.billing.repository.BillingRepository;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Billing;
import com.res.hms.domain.BillingSourceType;
import com.res.hms.domain.BillingStatus;
import com.res.hms.domain.Patient;
import com.res.hms.patient.service.PatientService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

  @Mock private BillingRepository billingRepository;
  @Mock private PatientService patientService;
  @Mock private HospitalContext hospitalContext;

  @InjectMocks private BillingService billingService;

  @Test
  void createOpdBillingPersistsDraftBilling() {
    Patient patient = new Patient();
    patient.setId(10L);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(patientService.requirePatient(10L, 4L)).thenReturn(patient);
    when(billingRepository.save(any(Billing.class)))
        .thenAnswer(
            invocation -> {
              Billing billing = invocation.getArgument(0);
              billing.setId(70L);
              return billing;
            });

    var response = billingService.createOpdBilling(new CreateOpdBillingRequest(10L));

    assertEquals(BillingStatus.DRAFT, response.status());
    assertEquals(BillingSourceType.OPD, response.sourceType());
  }

  @Test
  void addItemRejectsMissingBilling() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(billingRepository.findByIdAndHospitalId(99L, 4L)).thenReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                billingService.addItem(
                    99L, new AddBillingItemRequest("Consultation", new BigDecimal("50"), 1)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void addItemUpdatesTotalAndStatus() {
    Patient patient = new Patient();
    patient.setId(10L);
    Billing billing = new Billing();
    billing.setId(70L);
    billing.setPatient(patient);
    billing.setStatus(BillingStatus.DRAFT);
    billing.setTotalAmount(BigDecimal.ZERO);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(billingRepository.findByIdAndHospitalId(70L, 4L)).thenReturn(Optional.of(billing));
    when(billingRepository.save(any(Billing.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        billingService.addItem(
            70L, new AddBillingItemRequest("Consultation", new BigDecimal("50"), 2));

    assertEquals(new BigDecimal("100"), response.totalAmount());
    assertEquals(BillingStatus.GENERATED, response.status());
    assertEquals(1, response.items().size());
  }

  @Test
  void createAndAppendPharmacyBillingAccumulateTotals() {
    Patient patient = new Patient();
    patient.setId(10L);
    patient.setHospitalId(4L);
    Billing billing = new Billing();
    billing.setId(80L);
    billing.setPatient(patient);
    billing.setTotalAmount(BigDecimal.ZERO);
    when(billingRepository.save(any(Billing.class)))
        .thenAnswer(
            invocation -> {
              Billing persisted = invocation.getArgument(0);
              if (persisted.getId() == null) {
                persisted.setId(80L);
              }
              return persisted;
            });

    Billing created = billingService.createPharmacyBilling(patient);
    Billing updated =
        billingService.appendPharmacyItem(created, "Paracetamol", new BigDecimal("12.50"), 2);

    assertEquals(new BigDecimal("25.00"), updated.getTotalAmount());
    assertEquals(BillingStatus.GENERATED, updated.getStatus());
  }
}
