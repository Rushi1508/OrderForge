package com.orderforge.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository repository;

    @Transactional
    public boolean markIfNew(UUID eventId) {
        if (eventId == null) {
            log.warn("Event has null eventId; processing without dedup guard");
            return true;
        }
        int inserted = repository.insertIfAbsent(eventId);
        if (inserted == 0) {
            log.info("Duplicate event {} ignored", eventId);
            return false;
        }
        return true;
    }
}