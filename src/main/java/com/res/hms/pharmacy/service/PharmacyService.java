package com.res.hms.pharmacy.service;

import com.res.hms.billing.service.BillingService;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Billing;
import com.res.hms.domain.Medicine;
import com.res.hms.domain.PharmacyStock;
import com.res.hms.domain.Prescription;
import com.res.hms.domain.PrescriptionItem;
import com.res.hms.patient.service.PatientService;
import com.res.hms.pharmacy.dto.AddPharmacyStockRequest;
import com.res.hms.pharmacy.dto.AddPrescriptionItemRequest;
import com.res.hms.pharmacy.dto.CreateMedicineRequest;
import com.res.hms.pharmacy.dto.CreatePrescriptionRequest;
import com.res.hms.pharmacy.dto.DispensePrescriptionRequest;
import com.res.hms.pharmacy.dto.DispenseResponse;
import com.res.hms.pharmacy.dto.MedicineResponse;
import com.res.hms.pharmacy.dto.PharmacyStockResponse;
import com.res.hms.pharmacy.dto.PrescriptionResponse;
import com.res.hms.pharmacy.repository.MedicineRepository;
import com.res.hms.pharmacy.repository.PharmacyStockRepository;
import com.res.hms.pharmacy.repository.PrescriptionRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final PharmacyMapper pharmacyMapper;

  @Transactional
  @Auditable(entity = "medicine", action = "CREATE")
  public MedicineResponse createMedicine(CreateMedicineRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = new Medicine();
    medicine.setHospitalId(hospitalId);
    medicine.setName(request.name());
    medicine.setComposition(request.composition());
    medicine.setManufacturer(request.manufacturer());
    return pharmacyMapper.toMedicineResponse(medicineRepository.save(medicine));
  }

  public PageResponse<MedicineResponse> listMedicines(int page, int size) {
    Long hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        medicineRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
            .map(pharmacyMapper::toMedicineResponse));
  }

  @Transactional
  @Auditable(entity = "pharmacy_stock", action = "CREATE")
  public PharmacyStockResponse addStock(AddPharmacyStockRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine =
        medicineRepository
            .findByIdAndHospitalId(request.medicineId(), hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

    PharmacyStock stock = new PharmacyStock();
    stock.setHospitalId(hospitalId);
    stock.setMedicine(medicine);
    stock.setBatchNumber(request.batchNumber());
    stock.setExpiryDate(request.expiryDate());
    stock.setQuantity(request.quantity());
    stock.setPrice(request.price());
    return pharmacyMapper.toPharmacyStockResponse(pharmacyStockRepository.save(stock));
  }

  @Transactional(readOnly = true)
  public List<PharmacyStockResponse> listStock() {
    return pharmacyStockRepository
        .findAllByHospitalIdOrderByExpiryDateAsc(hospitalContext.requireHospitalId())
        .stream()
        .map(pharmacyMapper::toPharmacyStockResponse)
        .toList();
  }

  @Transactional
  @Auditable(entity = "prescription", action = "CREATE")
  public PrescriptionResponse createPrescription(CreatePrescriptionRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Prescription prescription = new Prescription();
    prescription.setHospitalId(hospitalId);
    prescription.setPatient(patientService.requirePatient(request.patientId(), hospitalId));
    prescription.setDoctor(doctorService.requireDoctor(request.doctorId(), hospitalId));
    return pharmacyMapper.toPrescriptionResponse(prescriptionRepository.save(prescription));
  }

  @Transactional
  @Auditable(entity = "prescription_item", action = "CREATE")
  public PrescriptionResponse addPrescriptionItem(
      Long prescriptionId, AddPrescriptionItemRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Prescription p =
        prescriptionRepository
            .findByIdAndHospitalId(prescriptionId, hospitalId)
            .orElseThrow(
                () ->
                    new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));

    Medicine m =
        medicineRepository
            .findByIdAndHospitalId(request.medicineId(), hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

    PrescriptionItem item = new PrescriptionItem();
    item.setHospitalId(hospitalId);
    item.setPrescription(p);
    item.setMedicine(m);
    item.setDosage(request.dosage());
    item.setDuration(request.duration());
    p.getItems().add(item);
    return pharmacyMapper.toPrescriptionResponse(prescriptionRepository.save(p));
  }

  @Transactional
  @Auditable(entity = "prescription", action = "DISPENSE")
  public DispenseResponse dispense(DispensePrescriptionRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Prescription prescription =
        prescriptionRepository
            .findByIdAndHospitalId(request.prescriptionId(), hospitalId)
            .orElseThrow(
                () ->
                    new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));

    if (prescription.getItems().isEmpty()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Nothing to dispense", HttpStatus.BAD_REQUEST);
    }

    Billing billing = billingService.createPharmacyBilling(prescription.getPatient());
    for (PrescriptionItem item : prescription.getItems()) {
      billing = dispenseItem(hospitalId, billing, item);
    }
    return new DispenseResponse(prescription.getId(), billingService.get(billing.getId()));
  }

  private Billing dispenseItem(Long hospitalId, Billing billing, PrescriptionItem item) {
    PharmacyStock stock = findAvailableStock(hospitalId, item.getMedicine());
    stock.setQuantity(stock.getQuantity() - 1);
    pharmacyStockRepository.save(stock);
    return billingService.appendPharmacyItem(
        billing, item.getMedicine().getName(), stock.getPrice(), 1);
  }

  private PharmacyStock findAvailableStock(Long hospitalId, Medicine medicine) {
    return pharmacyStockRepository
        .findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(hospitalId, medicine.getId())
        .stream()
        .filter(s -> s.getQuantity() > 0)
        .min(
            Comparator.comparing(
                PharmacyStock::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElseThrow(
            () ->
                new ApiException(
                    "BUSINESS_RULE_VIOLATION",
                    "Insufficient stock: " + medicine.getName(),
                    HttpStatus.BAD_REQUEST));
  }
}
