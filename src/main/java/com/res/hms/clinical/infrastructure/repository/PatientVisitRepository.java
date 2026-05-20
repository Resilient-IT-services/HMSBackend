package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.PatientVisit;
import com.res.hms.core.repository.BaseRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientVisitRepository extends BaseRepository<PatientVisit> {
  Optional<PatientVisit> findByHospitalIdAndVisitNumber(UUID hospitalId, String visitNumber);
}
