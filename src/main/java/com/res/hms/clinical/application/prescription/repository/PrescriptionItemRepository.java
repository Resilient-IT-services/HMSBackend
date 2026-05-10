package com.res.hms.clinical.application.prescription.repository;

import com.res.hms.clinical.domain.PrescriptionItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

  @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.id = :id AND pi.hospitalId = :hospitalId")
  Optional<PrescriptionItem> findByIdAndHospitalId(UUID id, UUID hospitalId);
}
