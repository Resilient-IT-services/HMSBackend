package com.res.hms.pharmacy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.billing.dto.BillingResponse;
import com.res.hms.billing.service.BillingService;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Billing;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Medicine;
import com.res.hms.domain.Patient;
import com.res.hms.domain.PharmacyStock;
import com.res.hms.domain.Prescription;
import com.res.hms.domain.PrescriptionItem;
import com.res.hms.domain.UserAccount;
import com.res.hms.patient.service.PatientService;
import com.res.hms.pharmacy.dto.AddPharmacyStockRequest;
import com.res.hms.pharmacy.dto.AddPrescriptionItemRequest;
import com.res.hms.pharmacy.dto.CreateMedicineRequest;
import com.res.hms.pharmacy.dto.CreatePrescriptionRequest;
import com.res.hms.pharmacy.dto.DispensePrescriptionRequest;
import com.res.hms.pharmacy.dto.MedicineResponse;
import com.res.hms.pharmacy.dto.PharmacyStockResponse;
import com.res.hms.pharmacy.dto.PrescriptionItemResponse;
import com.res.hms.pharmacy.dto.PrescriptionResponse;
import com.res.hms.pharmacy.repository.MedicineRepository;
import com.res.hms.pharmacy.repository.PharmacyStockRepository;
import com.res.hms.pharmacy.repository.PrescriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

  @Mock private MedicineRepository medicineRepository;
  @Mock private PharmacyStockRepository pharmacyStockRepository;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private PatientService patientService;
  @Mock private DoctorService doctorService;
  @Mock private BillingService billingService;
  @Mock private HospitalContext hospitalContext;
  @Mock private PharmacyMapper pharmacyMapper;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private PharmacyService pharmacyService;

  @Test
  void createMedicinePersistsMedicine() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(medicineRepository.save(any(Medicine.class)))
        .thenAnswer(
            invocation -> {
              Medicine medicine = invocation.getArgument(0);
              medicine.setId(90L);
              return medicine;
            });
    when(pharmacyMapper.toMedicineResponse(any(Medicine.class)))
        .thenAnswer(
            invocation -> {
              Medicine m = invocation.getArgument(0);
              return new MedicineResponse(
                  m.getId(), m.getName(), m.getComposition(), m.getManufacturer(), null);
            });

    var response =
        pharmacyService.createMedicine(
            new CreateMedicineRequest("Paracetamol", "Acetaminophen", "ACME"));

    assertEquals("Paracetamol", response.name());
  }

  @Test
  void listMedicinesReturnsMappedResults() {
    Medicine medicine = new Medicine();
    medicine.setId(1L);
    medicine.setName("Paracetamol");
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(medicineRepository.findAllByHospitalIdOrderByCreatedAtDesc(eq(4L), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(medicine)));
    when(pharmacyMapper.toMedicineResponse(any(Medicine.class)))
        .thenAnswer(
            invocation -> {
              Medicine m = invocation.getArgument(0);
              return new MedicineResponse(
                  m.getId(), m.getName(), m.getComposition(), m.getManufacturer(), null);
            });

    var response = pharmacyService.listMedicines(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("Paracetamol", response.content().getFirst().name());
  }

  @Test
  void addStockRejectsMissingMedicine() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(2L), eq(4L), any(), anyString()))
        .thenThrow(new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                pharmacyService.addStock(
                    new AddPharmacyStockRequest(
                        2L, "B1", LocalDate.now().plusMonths(3), 5, BigDecimal.TEN)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void addStockPersistsBatch() {
    Medicine medicine = new Medicine();
    medicine.setId(2L);
    medicine.setName("Paracetamol");
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(2L), eq(4L), any(), anyString())).thenReturn(medicine);
    when(pharmacyStockRepository.save(any(PharmacyStock.class)))
        .thenAnswer(
            invocation -> {
              PharmacyStock stock = invocation.getArgument(0);
              stock.setId(100L);
              return stock;
            });
    when(pharmacyMapper.toPharmacyStockResponse(any(PharmacyStock.class)))
        .thenAnswer(
            invocation -> {
              PharmacyStock s = invocation.getArgument(0);
              return new PharmacyStockResponse(
                  s.getId(),
                  s.getMedicine().getId(),
                  s.getMedicine().getName(),
                  s.getBatchNumber(),
                  s.getExpiryDate(),
                  s.getQuantity(),
                  s.getPrice());
            });

    var response =
        pharmacyService.addStock(
            new AddPharmacyStockRequest(
                2L, "B1", LocalDate.now().plusMonths(3), 5, new BigDecimal("12.50")));

    assertEquals("B1", response.batchNumber());
  }

  @Test
  void listStockReturnsMappedBatches() {
    Medicine medicine = new Medicine();
    medicine.setId(2L);
    medicine.setName("Paracetamol");
    PharmacyStock stock = new PharmacyStock();
    stock.setId(100L);
    stock.setMedicine(medicine);
    stock.setBatchNumber("B1");
    stock.setQuantity(5);
    stock.setPrice(new BigDecimal("12.50"));
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(pharmacyStockRepository.findAllByHospitalIdOrderByExpiryDateAsc(4L))
        .thenReturn(List.of(stock));
    when(pharmacyMapper.toPharmacyStockResponse(any(PharmacyStock.class)))
        .thenAnswer(
            invocation -> {
              PharmacyStock s = invocation.getArgument(0);
              return new PharmacyStockResponse(
                  s.getId(),
                  s.getMedicine().getId(),
                  s.getMedicine().getName(),
                  s.getBatchNumber(),
                  s.getExpiryDate(),
                  s.getQuantity(),
                  s.getPrice());
            });

    var response = pharmacyService.listStock();

    assertEquals(1, response.size());
    assertEquals("Paracetamol", response.getFirst().medicineName());
  }

  @Test
  void createPrescriptionPersistsPrescription() {
    Patient patient = new Patient();
    patient.setId(10L);
    patient.setFullName("Asha");

    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");
    Doctor doctor = new Doctor();
    doctor.setId(11L);
    doctor.setUser(user);

    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(patientService.requirePatient(10L, 4L)).thenReturn(patient);
    when(doctorService.requireDoctor(11L, 4L)).thenReturn(doctor);
    when(prescriptionRepository.save(any(Prescription.class)))
        .thenAnswer(
            invocation -> {
              Prescription prescription = invocation.getArgument(0);
              prescription.setId(110L);
              return prescription;
            });
    when(pharmacyMapper.toPrescriptionResponse(any(Prescription.class)))
        .thenAnswer(
            invocation -> {
              Prescription p = invocation.getArgument(0);
              return new PrescriptionResponse(
                  p.getId(),
                  p.getPatient().getId(),
                  p.getPatient().getFullName(),
                  p.getDoctor().getId(),
                  p.getDoctor().getFullName(),
                  List.of(),
                  null);
            });

    var response = pharmacyService.createPrescription(new CreatePrescriptionRequest(10L, 11L));

    assertEquals("Asha", response.patientName());
  }

  @Test
  void addPrescriptionItemAddsMedicineToPrescription() {
    Patient patient = new Patient();
    patient.setId(10L);
    patient.setFullName("Asha");

    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");
    Doctor doctor = new Doctor();
    doctor.setId(11L);
    doctor.setUser(user);

    Medicine medicine = new Medicine();
    medicine.setId(2L);
    medicine.setName("Paracetamol");
    Prescription prescription = new Prescription();
    prescription.setId(110L);
    prescription.setPatient(patient);
    prescription.setDoctor(doctor);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(110L), eq(4L), any(), anyString()))
        .thenReturn(prescription);
    when(resourceLookup.findRequired(eq(2L), eq(4L), any(), anyString())).thenReturn(medicine);
    when(prescriptionRepository.save(any(Prescription.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(pharmacyMapper.toPrescriptionResponse(any(Prescription.class)))
        .thenAnswer(
            invocation -> {
              Prescription p = invocation.getArgument(0);
              return new PrescriptionResponse(
                  p.getId(),
                  p.getPatient().getId(),
                  p.getPatient().getFullName(),
                  p.getDoctor().getId(),
                  p.getDoctor().getFullName(),
                  p.getItems().stream()
                      .map(
                          i ->
                              new PrescriptionItemResponse(
                                  i.getId(),
                                  i.getMedicine().getId(),
                                  i.getMedicine().getName(),
                                  i.getDosage(),
                                  i.getDuration()))
                      .toList(),
                  null);
            });

    var response =
        pharmacyService.addPrescriptionItem(
            110L, new AddPrescriptionItemRequest(2L, "1-0-1", "5 days"));

    assertEquals(1, response.items().size());
    assertEquals("Paracetamol", response.items().getFirst().medicineName());
  }

  @Test
  void dispenseRejectsEmptyPrescription() {
    Prescription prescription = new Prescription();
    prescription.setId(110L);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(110L), eq(4L), any(), anyString()))
        .thenReturn(prescription);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> pharmacyService.dispense(new DispensePrescriptionRequest(110L)));

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
  }

  @Test
  void dispenseRejectsInsufficientStock() {
    Patient patient = new Patient();
    patient.setId(10L);

    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");
    Doctor doctor = new Doctor();
    doctor.setId(11L);
    doctor.setUser(user);

    Medicine medicine = new Medicine();
    medicine.setId(2L);
    medicine.setName("Paracetamol");
    PrescriptionItem item = new PrescriptionItem();
    item.setMedicine(medicine);
    Prescription prescription = new Prescription();
    prescription.setId(110L);
    prescription.setPatient(patient);
    prescription.setDoctor(doctor);
    prescription.getItems().add(item);
    Billing billing = new Billing();
    billing.setId(200L);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(110L), eq(4L), any(), anyString()))
        .thenReturn(prescription);
    when(billingService.createPharmacyBilling(patient)).thenReturn(billing);
    when(pharmacyStockRepository.findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(4L, 2L))
        .thenReturn(List.of());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> pharmacyService.dispense(new DispensePrescriptionRequest(110L)));

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
  }

  @Test
  void dispenseConsumesEarliestValidBatchAndReturnsBilling() {
    Patient patient = new Patient();
    patient.setId(10L);
    patient.setHospitalId(4L);

    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");
    Doctor doctor = new Doctor();
    doctor.setId(11L);
    doctor.setUser(user);

    Medicine medicine = new Medicine();
    medicine.setId(2L);
    medicine.setName("Paracetamol");
    PrescriptionItem item = new PrescriptionItem();
    item.setMedicine(medicine);
    item.setDosage("1-0-1");
    item.setDuration("5 days");
    Prescription prescription = new Prescription();
    prescription.setId(110L);
    prescription.setPatient(patient);
    prescription.setDoctor(doctor);
    prescription.getItems().add(item);
    PharmacyStock exhausted = new PharmacyStock();
    exhausted.setMedicine(medicine);
    exhausted.setExpiryDate(LocalDate.now().plusDays(1));
    exhausted.setQuantity(0);
    exhausted.setPrice(new BigDecimal("11.00"));
    PharmacyStock usable = new PharmacyStock();
    usable.setMedicine(medicine);
    usable.setExpiryDate(LocalDate.now().plusDays(2));
    usable.setQuantity(2);
    usable.setPrice(new BigDecimal("12.50"));
    Billing billing = new Billing();
    billing.setId(200L);
    BillingResponse billingResponse =
        new BillingResponse(200L, 10L, null, null, new BigDecimal("12.50"), List.of(), null);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(110L), eq(4L), any(), anyString()))
        .thenReturn(prescription);
    when(billingService.createPharmacyBilling(patient)).thenReturn(billing);
    when(pharmacyStockRepository.findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(4L, 2L))
        .thenReturn(List.of(exhausted, usable));
    when(pharmacyStockRepository.save(any(PharmacyStock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(billingService.appendPharmacyItem(billing, "Paracetamol", new BigDecimal("12.50"), 1))
        .thenReturn(billing);
    when(billingService.get(200L)).thenReturn(billingResponse);

    var response = pharmacyService.dispense(new DispensePrescriptionRequest(110L));

    assertEquals(110L, response.prescriptionId());
    assertEquals(new BigDecimal("12.50"), response.billing().totalAmount());
    assertEquals(1, usable.getQuantity());
  }
}
