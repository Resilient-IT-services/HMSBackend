package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabOrderTest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LabOrderTestRepository
    extends JpaRepository<LabOrderTest, UUID>, JpaSpecificationExecutor<LabOrderTest> {
  
  @Query("SELECT new com.res.hms.lab.application.dto.LabQueueItemDTO(" +
         "o.id, ot.id, o.orderNumber, CAST(o.patientId AS string), " +
         "p.fullName, '', t.testName, ot.status, o.orderedAt) " +
         "FROM LabOrderTest ot " +
         "JOIN LabOrder o ON ot.orderId = o.id " +
         "JOIN com.res.hms.clinical.domain.Patient p ON o.patientId = p.id " +
         "LEFT JOIN LabTest t ON ot.testId = t.id " +
         "WHERE o.hospitalId = :hospitalId " +
         "AND (:status IS NULL OR ot.status = :status)")
  Page<com.res.hms.lab.application.dto.LabQueueItemDTO> findQueueByHospitalAndStatus(
      @Param("hospitalId") UUID hospitalId, 
      @Param("status") String status, 
      Pageable pageable);
}
