package com.res.hms.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Medicine;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicineRepository extends BaseRepository<Medicine> {
  Page<Medicine> findAllByHospitalIdAndNameContainingIgnoreCase(
      UUID hospitalId, String name, Pageable pageable);
}
