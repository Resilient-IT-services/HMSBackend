package com.res.hms.pharmacy.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.billing.dto.BillingResponse;
import com.res.hms.billing.service.BillingService;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Billing;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Medicine;
import com.res.hms.domain.Patient;
import com.res.hms.domain.PharmacyStock;
import com.res.hms.domain.Prescription;
import com.res.hms.domain.PrescriptionItem;
import com.res.hms.patient.service.PatientService;
import com.res.hms.pharmacy.dto.AddPharmacyStockRequest;
import com.res.hms.pharmacy.dto.AddPrescriptionItemRequest;
import com.res.hms.pharmacy.dto.CreateMedicineRequest;
import com.res.hms.pharmacy.dto.CreatePrescriptionRequest;
import com.res.hms.pharmacy.dto.DispensePrescriptionRequest;
import com.res.hms.pharmacy.repository.MedicineRepository;
import com.res.hms.pharmacy.repository.PharmacyStockRepository;
import com.res.hms.pharmacy.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private PharmacyStockRepository pharmacyStockRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private PatientService patientService;
    @Mock
    private DoctorService doctorService;
    @Mock
    private BillingService billingService;
    @Mock
    private HospitalContext hospitalContext;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PharmacyService pharmacyService;

    @Test
    void createMedicinePersistsMedicine() {
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(medicineRepository.save(any(Medicine.class))).thenAnswer(invocation -> {
            Medicine medicine = invocation.getArgument(0);
            medicine.setId(90L);
            return medicine;
        });

        var response = pharmacyService.createMedicine(new CreateMedicineRequest("Paracetamol", "Acetaminophen", "ACME"));

        assertEquals("Paracetamol", response.name());
        verify(auditLogService).record(4L, "medicine", 90L, "CREATE", "Created medicine Paracetamol");
    }

    @Test
    void listMedicinesReturnsMappedResults() {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("Paracetamol");
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(medicineRepository.findAllByHospitalIdOrderByCreatedAtDesc(eq(4L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(medicine)));

        var response = pharmacyService.listMedicines(0, 10);

        assertEquals(1, response.content().size());
        assertEquals("Paracetamol", response.content().getFirst().name());
    }

    @Test
    void addStockRejectsMissingMedicine() {
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(medicineRepository.findByIdAndHospitalId(2L, 4L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                pharmacyService.addStock(new AddPharmacyStockRequest(2L, "B1", LocalDate.now().plusMonths(3), 5, BigDecimal.TEN))
        );

        assertEquals("NOT_FOUND", exception.getCode());
    }

    @Test
    void addStockPersistsBatch() {
        Medicine medicine = new Medicine();
        medicine.setId(2L);
        medicine.setName("Paracetamol");
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(medicineRepository.findByIdAndHospitalId(2L, 4L)).thenReturn(Optional.of(medicine));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenAnswer(invocation -> {
            PharmacyStock stock = invocation.getArgument(0);
            stock.setId(100L);
            return stock;
        });

        var response = pharmacyService.addStock(new AddPharmacyStockRequest(
                2L, "B1", LocalDate.now().plusMonths(3), 5, new BigDecimal("12.50")
        ));

        assertEquals("B1", response.batchNumber());
        verify(auditLogService).record(4L, "pharmacy_stock", 100L, "CREATE", "Added pharmacy stock batch B1");
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
        when(pharmacyStockRepository.findAllByHospitalIdOrderByExpiryDateAsc(4L)).thenReturn(List.of(stock));

        var response = pharmacyService.listStock();

        assertEquals(1, response.size());
        assertEquals("Paracetamol", response.getFirst().medicineName());
    }

    @Test
    void createPrescriptionPersistsPrescription() {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setFullName("Asha");
        Doctor doctor = new Doctor();
        doctor.setId(11L);
        doctor.setFullName("Dr. Maya");
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(patientService.requirePatient(10L, 4L)).thenReturn(patient);
        when(doctorService.requireDoctor(11L, 4L)).thenReturn(doctor);
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription prescription = invocation.getArgument(0);
            prescription.setId(110L);
            return prescription;
        });

        var response = pharmacyService.createPrescription(new CreatePrescriptionRequest(10L, 11L));

        assertEquals("Asha", response.patientName());
        verify(auditLogService).record(4L, "prescription", 110L, "CREATE", "Created prescription");
    }

    @Test
    void addPrescriptionItemAddsMedicineToPrescription() {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setFullName("Asha");
        Doctor doctor = new Doctor();
        doctor.setId(11L);
        doctor.setFullName("Dr. Maya");
        Medicine medicine = new Medicine();
        medicine.setId(2L);
        medicine.setName("Paracetamol");
        Prescription prescription = new Prescription();
        prescription.setId(110L);
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(prescriptionRepository.findByIdAndHospitalId(110L, 4L)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findByIdAndHospitalId(2L, 4L)).thenReturn(Optional.of(medicine));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = pharmacyService.addPrescriptionItem(110L, new AddPrescriptionItemRequest(2L, "1-0-1", "5 days"));

        assertEquals(1, response.items().size());
        assertEquals("Paracetamol", response.items().getFirst().medicineName());
    }

    @Test
    void dispenseRejectsEmptyPrescription() {
        Prescription prescription = new Prescription();
        prescription.setId(110L);
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(prescriptionRepository.findByIdAndHospitalId(110L, 4L)).thenReturn(Optional.of(prescription));

        ApiException exception = assertThrows(ApiException.class, () ->
                pharmacyService.dispense(new DispensePrescriptionRequest(110L))
        );

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
    }

    @Test
    void dispenseRejectsInsufficientStock() {
        Patient patient = new Patient();
        patient.setId(10L);
        Doctor doctor = new Doctor();
        doctor.setId(11L);
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
        when(prescriptionRepository.findByIdAndHospitalId(110L, 4L)).thenReturn(Optional.of(prescription));
        when(billingService.createPharmacyBilling(patient)).thenReturn(billing);
        when(pharmacyStockRepository.findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(4L, 2L))
                .thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, () ->
                pharmacyService.dispense(new DispensePrescriptionRequest(110L))
        );

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
    }

    @Test
    void dispenseConsumesEarliestValidBatchAndReturnsBilling() {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setHospitalId(4L);
        Doctor doctor = new Doctor();
        doctor.setId(11L);
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
        BillingResponse billingResponse = new BillingResponse(
                200L, 10L, null, null, new BigDecimal("12.50"), List.of(), null
        );
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(prescriptionRepository.findByIdAndHospitalId(110L, 4L)).thenReturn(Optional.of(prescription));
        when(billingService.createPharmacyBilling(patient)).thenReturn(billing);
        when(pharmacyStockRepository.findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(4L, 2L))
                .thenReturn(List.of(exhausted, usable));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billingService.appendPharmacyItem(billing, "Paracetamol", new BigDecimal("12.50"), 1)).thenReturn(billing);
        when(billingService.get(200L)).thenReturn(billingResponse);

        var response = pharmacyService.dispense(new DispensePrescriptionRequest(110L));

        assertEquals(110L, response.prescriptionId());
        assertEquals(new BigDecimal("12.50"), response.billing().totalAmount());
        assertEquals(1, usable.getQuantity());
        verify(auditLogService).record(4L, "prescription", 110L, "DISPENSE", "Dispensed prescription and generated billing");
    }
}
