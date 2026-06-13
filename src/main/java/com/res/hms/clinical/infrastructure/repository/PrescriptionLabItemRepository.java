package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.PrescriptionLabItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescriptionLabItemRepository extends JpaRepository<PrescriptionLabItem, UUID> {
  Optional<PrescriptionLabItem> findByLabOrderTestId(UUID labOrderTestId);
}
