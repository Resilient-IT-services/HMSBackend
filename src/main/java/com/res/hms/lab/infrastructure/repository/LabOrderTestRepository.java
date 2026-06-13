package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabOrderTest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LabOrderTestRepository
    extends JpaRepository<LabOrderTest, UUID>, JpaSpecificationExecutor<LabOrderTest> {

  @Query(
      value =
          "SELECT new com.res.hms.lab.application.dto.LabQueueItemDTO("
              + "o.id, ot.id, o.orderNumber, o.patientId, "
              + "p.fullName, '', ot.testName, CAST(ot.category AS string), CAST(ot.resultEntryType AS string), ot.status, o.orderedAt) "
              + "FROM LabOrderTest ot, LabOrder o, com.res.hms.clinical.domain.Patient p "
              + "WHERE ot.orderId = o.id AND o.patientId = p.id "
              + "AND o.hospitalId = :hospitalId "
              + "AND (:status IS NULL OR ot.status = :status) "
              + "ORDER BY o.orderedAt DESC",
      countQuery =
          "SELECT count(ot) FROM LabOrderTest ot, LabOrder o "
              + "WHERE ot.orderId = o.id AND o.hospitalId = :hospitalId "
              + "AND (:status IS NULL OR ot.status = :status)")
  Page<com.res.hms.lab.application.dto.LabQueueItemDTO> findQueueByHospitalAndStatus(
      @Param("hospitalId") UUID hospitalId, @Param("status") String status, Pageable pageable);
}
