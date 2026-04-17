package com.res.hms.pharmacy.repository;

import com.res.hms.domain.PharmacyStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, Long> {
    List<PharmacyStock> findAllByHospitalIdAndMedicineIdOrderByExpiryDateAsc(Long hospitalId, Long medicineId);
    List<PharmacyStock> findAllByHospitalIdOrderByExpiryDateAsc(Long hospitalId);
}
