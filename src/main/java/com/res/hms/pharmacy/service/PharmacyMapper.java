package com.res.hms.pharmacy.service;

import com.res.hms.domain.Medicine;
import com.res.hms.domain.PharmacyStock;
import com.res.hms.domain.Prescription;
import com.res.hms.pharmacy.dto.MedicineResponse;
import com.res.hms.pharmacy.dto.PharmacyStockResponse;
import com.res.hms.pharmacy.dto.PrescriptionItemResponse;
import com.res.hms.pharmacy.dto.PrescriptionResponse;
import org.springframework.stereotype.Component;

@Component
public class PharmacyMapper {

  public MedicineResponse toMedicineResponse(Medicine medicine) {
    return new MedicineResponse(
        medicine.getId(),
        medicine.getName(),
        medicine.getComposition(),
        medicine.getManufacturer(),
        medicine.getCreatedAt());
  }

  public PharmacyStockResponse toPharmacyStockResponse(PharmacyStock stock) {
    return new PharmacyStockResponse(
        stock.getId(),
        stock.getMedicine().getId(),
        stock.getMedicine().getName(),
        stock.getBatchNumber(),
        stock.getExpiryDate(),
        stock.getQuantity(),
        stock.getPrice());
  }

  public PrescriptionResponse toPrescriptionResponse(Prescription prescription) {
    return new PrescriptionResponse(
        prescription.getId(),
        prescription.getPatient().getId(),
        prescription.getPatient().getFullName(),
        prescription.getDoctor().getId(),
        prescription.getDoctor().getFullName(),
        prescription.getItems().stream()
            .map(
                item ->
                    new PrescriptionItemResponse(
                        item.getId(),
                        item.getMedicine().getId(),
                        item.getMedicine().getName(),
                        item.getDosage(),
                        item.getDuration()))
            .toList(),
        prescription.getCreatedAt());
  }
}
