package com.res.hms.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.PharmacyStock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PharmacyStockRepository extends BaseRepository<PharmacyStock> {
  Page<PharmacyStock> findAllByHospitalIdAndQuantityLessThan(
      UUID hospitalId, Integer threshold, Pageable pageable);
}
