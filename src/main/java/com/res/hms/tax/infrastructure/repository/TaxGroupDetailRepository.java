package com.res.hms.tax.infrastructure.repository;

import com.res.hms.tax.domain.TaxGroup;
import com.res.hms.tax.domain.TaxGroupDetail;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxGroupDetailRepository extends JpaRepository<TaxGroupDetail, UUID> {
    void deleteByGroup(TaxGroup group);
}
