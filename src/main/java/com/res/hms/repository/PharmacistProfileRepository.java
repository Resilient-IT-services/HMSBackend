package com.res.hms.repository;

import com.res.hms.domain.PharmacistProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PharmacistProfileRepository extends JpaRepository<PharmacistProfile, UUID> {
  boolean existsByPciRegNo(String pciRegNo);

  boolean existsByPciRegNoAndIdNot(String pciRegNo, UUID id);
}
