package com.res.hms.repository;

import com.res.hms.domain.LabTechProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabTechProfileRepository extends JpaRepository<LabTechProfile, UUID> {
  boolean existsByLicenseNo(String licenseNo);

  boolean existsByLicenseNoAndIdNot(String licenseNo, UUID id);
}
