package com.res.hms.core.repository;

import com.res.hms.core.domain.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
  List<OutboxEvent> findByProcessedAtIsNullOrderByCreatedAtAsc();
}
