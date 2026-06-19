package com.orderforge.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishOutbox() {
        List<OutboxEvent> batch = outboxRepository.findUnpublished(PageRequest.of(0, 100));
        if (batch.isEmpty()) return;

        for (OutboxEvent row : batch) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    row.getTopic(),
                    row.getAggregateId(),
                    row.getPayload());
            record.headers().add(new RecordHeader(
                    "__TypeId__",
                    row.getTypeId().getBytes(StandardCharsets.UTF_8)));

            outboxKafkaTemplate.send(record);
            row.setPublishedAt(Instant.now());
            outboxRepository.save(row);
            log.info("Published outbox row {} ({}) to topic {}",
                    row.getId(), row.getEventType(), row.getTopic());
        }
    }
}