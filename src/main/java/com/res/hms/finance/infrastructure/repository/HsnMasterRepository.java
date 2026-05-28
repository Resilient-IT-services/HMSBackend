package com.res.hms.finance.infrastructure.repository;

import com.res.hms.finance.domain.HsnMaster;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HsnMasterRepository extends JpaRepository<HsnMaster, UUID> {
  List<HsnMaster> findAllByHospitalIdAndActiveTrue(UUID hospitalId);

  Optional<HsnMaster> findByHospitalIdAndHsnCodeAndActiveTrue(UUID hospitalId, String hsnCode);
}
