package com.res.hms.ipd.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.ipd.domain.Ward;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends BaseRepository<Ward> {
  Optional<Ward> findByWardCodeAndHospitalId(String wardCode, UUID hospitalId);
}
