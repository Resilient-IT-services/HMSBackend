package com.res.hms.pharmacy.infrastructure.repository;

import com.res.hms.pharmacy.domain.HospitalMedicineCatalog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalMedicineCatalogRepository extends JpaRepository<HospitalMedicineCatalog, UUID> {
  Optional<HospitalMedicineCatalog> findByHospitalIdAndMedicineId(UUID hospitalId, UUID medicineId);
}
