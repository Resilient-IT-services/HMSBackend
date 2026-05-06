package com.res.hms.repository;

import com.res.hms.domain.PharmacySaleItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacySaleItemRepository extends JpaRepository<PharmacySaleItem, UUID> {

  @Query(
      """
      SELECT i FROM PharmacySaleItem i
      JOIN FETCH i.sale s
      JOIN FETCH i.stock st
      WHERE i.id = :id AND s.hospitalId = :hospitalId
      """)
  Optional<PharmacySaleItem> findByIdAndHospitalId(
      @Param("id") UUID id, @Param("hospitalId") UUID hospitalId);
}
