package com.res.hms.repository;

import com.res.hms.domain.PharmacistProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PharmacistProfileRepository extends JpaRepository<PharmacistProfile, UUID> {
  boolean existsByPciRegNo(String pciRegNo);

  boolean existsByPciRegNoAndIdNot(String pciRegNo, UUID id);

  @org.springframework.data.jpa.repository.Query(
      "SELECT COUNT(p) > 0 FROM PharmacistProfile p WHERE p.pciRegNo = :pciRegNo AND p.user.id <> :userId")
  boolean existsByPciRegNoAndUserIdNot(
      @org.springframework.data.repository.query.Param("pciRegNo") String pciRegNo,
      @org.springframework.data.repository.query.Param("userId") java.util.UUID userId);
}
