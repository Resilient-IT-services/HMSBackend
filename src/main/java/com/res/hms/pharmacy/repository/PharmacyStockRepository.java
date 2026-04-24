package com.res.hms.pharmacy.repository;

import com.res.hms.domain.PharmacyStock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, UUID> {
  List<PharmacyStock> findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(
      UUID hospitalId, UUID medicineId);

  List<PharmacyStock> findAllByHospitalIdOrderByExpiryDateAsc(UUID hospitalId);
}
