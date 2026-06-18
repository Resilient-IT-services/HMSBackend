package com.res.hms.ipd.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.ipd.domain.Bed;
import com.res.hms.ipd.domain.BedStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface BedRepository extends BaseRepository<Bed> {
  List<Bed> findAllByWardIdAndHospitalId(UUID wardId, UUID hospitalId);

  List<Bed> findAllByHospitalIdAndStatus(UUID hospitalId, BedStatus status);
}
