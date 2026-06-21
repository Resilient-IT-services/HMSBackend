package com.res.hms.ipd.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.ipd.domain.BedTransferHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface BedTransferHistoryRepository extends BaseRepository<BedTransferHistory> {
  List<BedTransferHistory> findAllByAdmissionIdAndHospitalIdOrderByTransferredAtDesc(
      UUID admissionId, UUID hospitalId);
}
