package com.res.hms.iam.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.res.hms.iam.domain.PharmacistProfile;

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
