package com.orderforge.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration TTL = Duration.ofHours(24);

    /**
     * Atomically records an event as processed using Redis SET NX.
     * @return true if first time seen (proceed), false if duplicate (skip).
     */
    public boolean markIfNew(UUID eventId) {
        if (eventId == null) {
            log.warn("Event has null eventId; processing without dedup guard");
            return true;
        }
        String key = "processed-event:" + eventId;
        Boolean set = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", TTL);
        if (Boolean.TRUE.equals(set)) {
            return true;
        }
        log.info("Duplicate event {} ignored", eventId);
        return false;
    }
}