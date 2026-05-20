package com.res.hms.pharmacy.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.pharmacy.domain.PharmacySale;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacySaleRepository extends BaseRepository<PharmacySale> {

  @Query(
      """
      SELECT DISTINCT s.customerName, s.customerPhone
      FROM PharmacySale s
      WHERE s.hospitalId = :hospitalId
        AND s.saleType = 'WALKIN'
        AND s.customerName IS NOT NULL
        AND (LOWER(s.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR s.customerPhone LIKE CONCAT('%', :search, '%'))
      ORDER BY s.customerName ASC
      """)
  List<Object[]> searchRecentWalkinCustomers(
      @Param("hospitalId") UUID hospitalId, @Param("search") String search);

  java.util.Optional<PharmacySale> findByIdAndHospitalId(UUID id, UUID hospitalId);

  java.util.Optional<PharmacySale> findByPrescriptionIdAndHospitalId(
      UUID prescriptionId, UUID hospitalId);
}
