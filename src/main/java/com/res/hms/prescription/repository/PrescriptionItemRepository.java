package com.res.hms.prescription.repository;

import com.res.hms.domain.PrescriptionItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

  @Query(
      "SELECT pi FROM PrescriptionItem pi WHERE pi.id = :id AND pi.prescription.appointment.hospitalId = :hospitalId")
  Optional<PrescriptionItem> findByIdAndHospitalId(UUID id, UUID hospitalId);
}
