package com.res.hms.iam.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.res.hms.iam.domain.LabTechProfile;

@Repository
public interface LabTechProfileRepository extends JpaRepository<LabTechProfile, UUID> {
  boolean existsByLicenseNo(String licenseNo);

  boolean existsByLicenseNoAndIdNot(String licenseNo, UUID id);

  @org.springframework.data.jpa.repository.Query(
      "SELECT COUNT(p) > 0 FROM LabTechProfile p WHERE p.licenseNo = :licenseNo AND p.user.id <> :userId")
  boolean existsByLicenseNoAndUserIdNot(
      @org.springframework.data.repository.query.Param("licenseNo") String licenseNo,
      @org.springframework.data.repository.query.Param("userId") java.util.UUID userId);
}
