package com.res.hms.tax.infrastructure.repository;

import com.res.hms.tax.domain.TaxGroup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxGroupRepository extends JpaRepository<TaxGroup, UUID> {
}
