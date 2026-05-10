package com.res.hms.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.core.domain.OutboxEvent;
import com.res.hms.core.repository.OutboxRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainEventPublisher {
  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  /** Persists the event in the outbox table. MUST be called inside an existing transaction. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void publish(String aggregateType, UUID aggregateId, Object event) {
    try {
      OutboxEvent outbox = new OutboxEvent();
      outbox.setAggregateType(aggregateType);
      outbox.setAggregateId(aggregateId);
      outbox.setEventType(event.getClass().getSimpleName());
      outbox.setPayload(objectMapper.writeValueAsString(event));
      outboxRepository.save(outbox);

      // Immediately trigger the in-memory Spring event as well
      // for subscribers that use @TransactionalEventListener(phase = AFTER_COMMIT)
      eventPublisher.publishEvent(event);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize domain event", e);
    }
  }
}
