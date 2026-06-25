package com.res.hms.ipd.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.ipd.domain.ProgressNote;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressNoteRepository extends BaseRepository<ProgressNote> {
  List<ProgressNote> findAllByAdmissionIdAndHospitalIdOrderByCreatedAtDesc(
      UUID admissionId, UUID hospitalId);
}
