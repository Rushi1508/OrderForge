package com.orderforge.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    /**
     * Writes an event to the outbox table. MUST be called within an existing
     * transaction (e.g. from a @Transactional reserve method) so the outbox row
     * commits atomically with the business change.
     */
    public void stage(String aggregateType, String aggregateId, String eventType,
                      String topic, String typeId, Object event) {
        String payload = jsonMapper.writeValueAsString(event);
        OutboxEvent row = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .payload(payload)
                .typeId(typeId)
                .createdAt(Instant.now())
                .publishedAt(null)
                .build();
        outboxRepository.save(row);
        log.info("Staged {} to outbox for aggregate {} (topic {})", eventType, aggregateId, topic);
    }
}