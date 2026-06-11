package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabOrder;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabOrderRepository
    extends JpaRepository<LabOrder, UUID>, JpaSpecificationExecutor<LabOrder> {
  java.util.List<LabOrder> findByPatientId(UUID patientId);

  long countByHospitalIdAndStatusAndOrderedAtGreaterThanEqual(
      UUID hospitalId, String status, java.time.Instant orderedAt);
}
