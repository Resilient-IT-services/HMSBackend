package com.res.hms.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.res.hms.core.domain.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByProcessedAtIsNullOrderByCreatedAtAsc();
}
