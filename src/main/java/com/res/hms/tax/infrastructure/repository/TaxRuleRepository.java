package com.res.hms.tax.infrastructure.repository;

import com.res.hms.tax.domain.TaxRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, UUID> {
  @Query("SELECT r FROM TaxRule r WHERE r.itemType = :itemType " +
         "AND (r.hsnCode IS NULL OR r.hsnCode = :hsnCode) " +
         "AND r.effectiveFrom <= CURRENT_TIMESTAMP " +
         "AND (r.effectiveTo IS NULL OR r.effectiveTo >= CURRENT_TIMESTAMP) " +
         "ORDER BY r.priority DESC")
  List<TaxRule> findActiveRulesByItemTypeAndHsn(
      @Param("itemType") String itemType, 
      @Param("hsnCode") String hsnCode);
}
