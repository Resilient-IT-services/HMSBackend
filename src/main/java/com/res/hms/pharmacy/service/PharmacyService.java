package com.res.hms.pharmacy.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.billing.dto.BillingResponse;
import com.res.hms.billing.service.BillingService;
import com.res.hms.common.api.PageResponse;
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
import com.res.hms.pharmacy.dto.DispenseResponse;
import com.res.hms.pharmacy.dto.DispensePrescriptionRequest;
import com.res.hms.pharmacy.dto.MedicineResponse;
import com.res.hms.pharmacy.dto.PharmacyStockResponse;
import com.res.hms.pharmacy.dto.PrescriptionItemResponse;
import com.res.hms.pharmacy.dto.PrescriptionResponse;
import com.res.hms.pharmacy.repository.MedicineRepository;
import com.res.hms.pharmacy.repository.PharmacyStockRepository;
import com.res.hms.pharmacy.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PharmacyService {

    private final MedicineRepository medicineRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final BillingService billingService;
    private final HospitalContext hospitalContext;
    private final AuditLogService auditLogService;

    @Transactional
    public MedicineResponse createMedicine(CreateMedicineRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();
        Medicine medicine = new Medicine();
        medicine.setHospitalId(hospitalId);
        medicine.setName(request.name());
        medicine.setComposition(request.composition());
        medicine.setManufacturer(request.manufacturer());
        medicineRepository.save(medicine);

        auditLogService.record(hospitalId, "medicine", medicine.getId(), "CREATE", "Created medicine " + medicine.getName());
        return toMedicineResponse(medicine);
    }

    public PageResponse<MedicineResponse> listMedicines(int page, int size) {
        Long hospitalId = hospitalContext.requireHospitalId();
        return PageResponse.from(medicineRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
                .map(this::toMedicineResponse));
    }

    @Transactional
    public PharmacyStockResponse addStock(AddPharmacyStockRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();
        Medicine medicine = medicineRepository.findByIdAndHospitalId(request.medicineId(), hospitalId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

        PharmacyStock stock = new PharmacyStock();
        stock.setHospitalId(hospitalId);
        stock.setMedicine(medicine);
        stock.setBatchNumber(request.batchNumber());
        stock.setExpiryDate(request.expiryDate());
        stock.setQuantity(request.quantity());
        stock.setPrice(request.price());
        pharmacyStockRepository.save(stock);

        auditLogService.record(hospitalId, "pharmacy_stock", stock.getId(), "CREATE", "Added pharmacy stock batch " + stock.getBatchNumber());
        return toPharmacyStockResponse(stock);
    }

    @Transactional(readOnly = true)
    public List<PharmacyStockResponse> listStock() {
        Long hospitalId = hospitalContext.requireHospitalId();
        return pharmacyStockRepository.findAllByHospitalIdOrderByExpiryDateAsc(hospitalId).stream()
                .map(this::toPharmacyStockResponse)
                .toList();
    }

    @Transactional
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();
        Patient patient = patientService.requirePatient(request.patientId(), hospitalId);
        Doctor doctor = doctorService.requireDoctor(request.doctorId(), hospitalId);

        Prescription prescription = new Prescription();
        prescription.setHospitalId(hospitalId);
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescriptionRepository.save(prescription);

        auditLogService.record(hospitalId, "prescription", prescription.getId(), "CREATE", "Created prescription");
        return toPrescriptionResponse(prescription);
    }

    @Transactional
    public PrescriptionResponse addPrescriptionItem(Long prescriptionId, AddPrescriptionItemRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();
        Prescription prescription = requirePrescription(prescriptionId, hospitalId);
        Medicine medicine = medicineRepository.findByIdAndHospitalId(request.medicineId(), hospitalId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

        PrescriptionItem item = new PrescriptionItem();
        item.setHospitalId(hospitalId);
        item.setPrescription(prescription);
        item.setMedicine(medicine);
        item.setDosage(request.dosage());
        item.setDuration(request.duration());
        prescription.getItems().add(item);
        prescriptionRepository.save(prescription);

        auditLogService.record(hospitalId, "prescription_item", prescription.getId(), "CREATE", "Added medicine " + medicine.getName() + " to prescription");
        return toPrescriptionResponse(prescription);
    }

    @Transactional
    public DispenseResponse dispense(DispensePrescriptionRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();
        Prescription prescription = requirePrescription(request.prescriptionId(), hospitalId);
        if (prescription.getItems().isEmpty()) {
            throw new ApiException("BUSINESS_RULE_VIOLATION", "Prescription has no items to dispense", HttpStatus.BAD_REQUEST);
        }

        // For Phase 1 we dispense one unit per prescribed medicine item and consume the earliest-expiring batch first.
        Billing billing = billingService.createPharmacyBilling(prescription.getPatient());
        for (PrescriptionItem item : prescription.getItems()) {
            List<PharmacyStock> stockEntries = pharmacyStockRepository.findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(hospitalId, item.getMedicine().getId());
            PharmacyStock stock = stockEntries.stream()
                    .filter(entry -> entry.getQuantity() > 0)
                    .min(Comparator.comparing(PharmacyStock::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElseThrow(() -> new ApiException(
                            "BUSINESS_RULE_VIOLATION",
                            "Insufficient stock for medicine " + item.getMedicine().getName(),
                            HttpStatus.BAD_REQUEST
                    ));

            stock.setQuantity(stock.getQuantity() - 1);
            pharmacyStockRepository.save(stock);
            billing = billingService.appendPharmacyItem(billing, item.getMedicine().getName(), stock.getPrice(), 1);
        }

        BillingResponse finalBilling = billingService.get(billing.getId());
        auditLogService.record(hospitalId, "prescription", prescription.getId(), "DISPENSE", "Dispensed prescription and generated billing");
        return new DispenseResponse(prescription.getId(), finalBilling);
    }

    private Prescription requirePrescription(Long prescriptionId, Long hospitalId) {
        return prescriptionRepository.findByIdAndHospitalId(prescriptionId, hospitalId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));
    }

    private MedicineResponse toMedicineResponse(Medicine medicine) {
        return new MedicineResponse(
                medicine.getId(),
                medicine.getName(),
                medicine.getComposition(),
                medicine.getManufacturer(),
                medicine.getCreatedAt()
        );
    }

    private PharmacyStockResponse toPharmacyStockResponse(PharmacyStock stock) {
        return new PharmacyStockResponse(
                stock.getId(),
                stock.getMedicine().getId(),
                stock.getMedicine().getName(),
                stock.getBatchNumber(),
                stock.getExpiryDate(),
                stock.getQuantity(),
                stock.getPrice()
        );
    }

    private PrescriptionResponse toPrescriptionResponse(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPatient().getId(),
                prescription.getPatient().getFullName(),
                prescription.getDoctor().getId(),
                prescription.getDoctor().getFullName(),
                prescription.getItems().stream().map(item -> new PrescriptionItemResponse(
                        item.getId(),
                        item.getMedicine().getId(),
                        item.getMedicine().getName(),
                        item.getDosage(),
                        item.getDuration()
                )).toList(),
                prescription.getCreatedAt()
        );
    }
}
