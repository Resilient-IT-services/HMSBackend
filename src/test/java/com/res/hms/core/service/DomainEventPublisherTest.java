package com.res.hms.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.hms.core.domain.OutboxEvent;
import com.res.hms.core.repository.OutboxRepository;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock private OutboxRepository outboxRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DomainEventPublisher domainEventPublisher;

    @Test
    void testPublish_Success() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        Object event = new Object();
        String payload = "{}";

        when(objectMapper.writeValueAsString(event)).thenReturn(payload);

        domainEventPublisher.publish("test", aggregateId, event);

        verify(outboxRepository).save(any(OutboxEvent.class));
        verify(eventPublisher).publishEvent(event);
    }

    @Test
    void testPublish_SerializationError() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> domainEventPublisher.publish("test", UUID.randomUUID(), new Object()));
    }
}
