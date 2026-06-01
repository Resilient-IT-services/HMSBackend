package com.res.hms.tax.infrastructure.repository;

import com.res.hms.tax.domain.TaxMaster;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxMasterRepository extends JpaRepository<TaxMaster, UUID> {
  Optional<TaxMaster> findByCode(String code);
}
