package com.res.hms.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.billing.dto.AddBillingItemRequest;
import com.res.hms.billing.dto.CreateOpdBillingRequest;
import com.res.hms.billing.repository.BillingRepository;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Billing;
import com.res.hms.domain.BillingSourceType;
import com.res.hms.domain.BillingStatus;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Patient;
import com.res.hms.patient.service.PatientService;
import com.res.hms.pharmacy.repository.PrescriptionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
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
  @Mock private ResourceLookup resourceLookup;
  @Mock private DoctorService doctorService;
  @Mock private PrescriptionRepository prescriptionRepository;

  @InjectMocks private BillingService billingService;

  @Test
  @SuppressWarnings("unchecked")
  void createOpdBillingPersistsDraftBilling() {
    Patient patient = new Patient();
    patient.setId(UUID.nameUUIDFromBytes("10".getBytes()));
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(patientService.requirePatient(
            UUID.nameUUIDFromBytes("10".getBytes()), UUID.nameUUIDFromBytes("4".getBytes())))
        .thenReturn(patient);
    Doctor doctor = new Doctor();
    doctor.setId(UUID.nameUUIDFromBytes("20".getBytes()));
    doctor.setConsultationFee(new BigDecimal("100"));
    when(doctorService.requireDoctor(
            UUID.nameUUIDFromBytes("20".getBytes()), UUID.nameUUIDFromBytes("4".getBytes())))
        .thenReturn(doctor);

    when(prescriptionRepository.findTopByPatient_IdAndDoctor_IdOrderByCreatedAtDesc(
            UUID.nameUUIDFromBytes("10".getBytes()), UUID.nameUUIDFromBytes("20".getBytes())))
        .thenReturn(Optional.empty());
    when(billingRepository.save(any(Billing.class)))
        .thenAnswer(
            invocation -> {
              Billing billing = invocation.getArgument(0);
              billing.setId(UUID.nameUUIDFromBytes("70".getBytes()));
              return billing;
            });

    var response =
        billingService.createOpdBilling(
            new CreateOpdBillingRequest(
                UUID.nameUUIDFromBytes("10".getBytes()), UUID.nameUUIDFromBytes("20".getBytes())));

    assertEquals(BillingStatus.DRAFT, response.status());
    assertEquals(BillingSourceType.OPD, response.sourceType());
  }

  @Test
  @SuppressWarnings("unchecked")
  void addItemRejectsMissingBilling() {
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(resourceLookup.findRequired(
            eq(UUID.nameUUIDFromBytes("99".getBytes())),
            eq(UUID.nameUUIDFromBytes("4".getBytes())),
            any(),
            anyString()))
        .thenThrow(
            new ApiException(
                "NOT_FOUND", "Billing not found", org.springframework.http.HttpStatus.NOT_FOUND));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                billingService.addItem(
                    UUID.nameUUIDFromBytes("99".getBytes()),
                    new AddBillingItemRequest("Consultation", new BigDecimal("50"), 1)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void addItemUpdatesTotalAndStatus() {
    Patient patient = new Patient();
    patient.setId(UUID.nameUUIDFromBytes("10".getBytes()));
    Billing billing = new Billing();
    billing.setId(UUID.nameUUIDFromBytes("70".getBytes()));
    billing.setPatient(patient);
    billing.setStatus(BillingStatus.DRAFT);
    billing.setTotalAmount(BigDecimal.ZERO);
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(resourceLookup.findRequired(
            eq(UUID.nameUUIDFromBytes("70".getBytes())),
            eq(UUID.nameUUIDFromBytes("4".getBytes())),
            any(),
            anyString()))
        .thenReturn(billing);
    when(billingRepository.save(any(Billing.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        billingService.addItem(
            UUID.nameUUIDFromBytes("70".getBytes()),
            new AddBillingItemRequest("Consultation", new BigDecimal("50"), 2));

    assertEquals(new BigDecimal("100"), response.totalAmount());
    assertEquals(BillingStatus.GENERATED, response.status());
    assertEquals(1, response.items().size());
  }

  @Test
  void createAndAppendPharmacyBillingAccumulateTotals() {
    Patient patient = new Patient();
    patient.setId(UUID.nameUUIDFromBytes("10".getBytes()));
    patient.setHospitalId(UUID.nameUUIDFromBytes("4".getBytes()));
    Billing billing = new Billing();
    billing.setId(UUID.nameUUIDFromBytes("80".getBytes()));
    billing.setPatient(patient);
    billing.setTotalAmount(BigDecimal.ZERO);
    when(billingRepository.save(any(Billing.class)))
        .thenAnswer(
            invocation -> {
              Billing persisted = invocation.getArgument(0);
              if (persisted.getId() == null) {
                persisted.setId(UUID.nameUUIDFromBytes("80".getBytes()));
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
